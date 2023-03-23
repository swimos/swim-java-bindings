mod writer;

use crate::bindings::java::writer::Writer;
use crate::bindings::MACRO_PATH;
use crate::docs::{Documentation, FormatStyle};
use crate::{JavaType, PrimitiveJavaType};
use heck::AsUpperCamelCase;
use lazy_static::lazy_static;
use proc_macro2::{Ident, Span};
use quote::ToTokens;
use std::collections::{HashSet, VecDeque};
use std::fmt::{Display, Formatter};
use std::io;
use std::io::Write;
use syn::spanned::Spanned;
use syn::{
    Attribute, Error, Field, Fields, ItemEnum, ItemStruct, Lit, Meta, NestedMeta, PathArguments,
    PathSegment, Type, TypePath,
};
pub use writer::{JavaSourceWriter, JavaSourceWriterBuilder};

const BUFFER_SIZE_VAR: &str = "__buf__size";
const BUFFER_VAR: &str = "__buf";
const AS_BYTES_METHOD: &str = "asBytes";

lazy_static! {
    static ref JAVA_KEYWORDS: HashSet<&'static str> = {
        let mut set = HashSet::from([
            "abstract",
            "assert",
            "boolean",
            "break ",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "double",
            "do",
            "else",
            "enum",
            "extends",
            "false",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "null",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "true",
            "try",
            "void",
            "volatile",
            "while",
            BUFFER_SIZE_VAR,
            AS_BYTES_METHOD,
        ]);
        set
    };
}

#[derive(Debug)]
pub enum JavaBinding {
    Class(ClassBinding),
    AbstractClass(AbstractClassBinding),
}

impl JavaBinding {
    pub fn derive_struct(
        infer_docs: bool,
        root_doc: Documentation,
        item: ItemStruct,
    ) -> Result<JavaBinding, Error> {
        Ok(JavaBinding::Class(derive_class(
            infer_docs, root_doc, item,
        )?))
    }

    pub fn derive_enum(
        _infer_docs: bool,
        _root_doc: Documentation,
        _item: ItemEnum,
    ) -> Result<JavaBinding, Error> {
        // let mut variants = Vec::new();
        // for variant in item.variants {
        //     variants.push(derive_class(infer_docs, root_doc,&variant.ident, &variant.fields)?);
        // }
        // Ok(JavaBinding::AbstractClass(AbstractClassBinding {
        //     class_name: item.ident.to_string(),
        //     documentation: root_doc,
        //     variants,
        // }))
        unimplemented!()
    }

    pub fn write(self, writer: &mut JavaSourceWriter) -> io::Result<()> {
        match self {
            JavaBinding::Class(binding) => binding.write(writer),
            JavaBinding::AbstractClass(binding) => binding.write(writer),
        }
    }
}

fn derive_class(
    infer_docs: bool,
    root_doc: Documentation,
    source: ItemStruct,
) -> Result<ClassBinding, Error> {
    Ok(ClassBinding {
        class_name: source.ident.to_string(),
        documentation: root_doc,
        fields: source.fields.iter().try_fold(
            Vec::new(),
            |mut fields, field| match derive_field(infer_docs, field) {
                Ok(field) => {
                    fields.push(field);
                    Ok(fields)
                }
                Err(e) => Err(e),
            },
        )?,
        source,
    })
}

fn derive_field(infer_docs: bool, field: &Field) -> Result<JavaField, Error> {
    let ty = map_type(&field.ty)?;
    let properties = derive_field_properties(infer_docs, ty, &field.attrs)?;
    let span = field.span();
    let default_value = match properties.default_value {
        Some(ty) => ty,
        None => ty.default_value(),
    };
    let name = field
        .ident
        .as_ref()
        .ok_or(Error::new(span, "Tuple fields are not supported"))
        .map(|i| i.to_string())?;
    sanitize_name(name.as_str(), span)?;

    Ok(JavaField {
        name,
        documentation: properties.documentation,
        ty,
        default_value,
    })
}

fn sanitize_name(name: &str, span: Span) -> Result<(), Error> {
    if JAVA_KEYWORDS.contains(name) {
        Err(Error::new(
            span,
            format!(
                "Attempted to use a reserved keyword as a field name: {}",
                name
            ),
        ))
    } else {
        Ok(())
    }
}

pub fn map_type(ty: &Type) -> Result<JavaType, Error> {
    let unsupported_type = |span| Err(Error::new(span, "Unsupported type"));
    match ty {
        Type::Path(TypePath { qself: None, path }) => match path.segments.last() {
            Some(PathSegment {
                ident,
                arguments: PathArguments::None,
            }) => match JavaType::try_from(ident.to_string().as_str()) {
                Ok(ty) => Ok(ty),
                Err(e) => Err(Error::new(path.span(), e)),
            },
            Some(meta) => unsupported_type(meta.span()),
            None => unsupported_type(Span::call_site()),
        },
        meta => unsupported_type(meta.span()),
    }
}

struct FieldProperties {
    documentation: Documentation,
    default_value: Option<String>,
}

fn derive_field_properties(
    infer_docs: bool,
    field_type: JavaType,
    attrs: &[Attribute],
) -> Result<FieldProperties, Error> {
    let mut properties = FieldProperties {
        documentation: Documentation::from_style(FormatStyle::Documentation),
        default_value: None,
    };

    if !infer_docs {
        return Ok(properties);
    }

    let mut attribute_iter = attrs
        .iter()
        .filter(|at| at.path.is_ident(MACRO_PATH))
        .peekable();

    if attribute_iter.peek().is_none() {
        return Ok(properties);
    }

    let unknown_attribute = |span| Err(Error::new(span, "Unknown attribute"));

    for attr in attribute_iter {
        match attr.parse_meta()? {
            Meta::List(args) => {
                for nested in args.nested {
                    match nested {
                        NestedMeta::Meta(Meta::NameValue(meta)) if meta.path.is_ident("doc") => {
                            match meta.lit {
                                Lit::Str(str) => properties.documentation.push_line(str.value()),
                                meta => return unknown_attribute(meta.span()),
                            }
                        }
                        NestedMeta::Meta(Meta::NameValue(meta))
                            if meta.path.is_ident("default_value") =>
                        {
                            if properties.default_value.is_some() {
                                return Err(Error::new(meta.span(), "Duplicate default value"));
                            }
                            properties.default_value =
                                Some(field_type.unpack_default_value(meta.lit)?);
                        }
                        meta => {
                            return unknown_attribute(meta.span());
                        }
                    }
                }
            }
            meta => return unknown_attribute(meta.span()),
        }
    }

    Ok(properties)
}

#[derive(Debug)]
pub struct ClassBinding {
    class_name: String,
    documentation: Documentation,
    fields: Vec<JavaField>,
    source: ItemStruct,
}

impl ClassBinding {
    pub fn write(self, writer: &mut JavaSourceWriter) -> io::Result<()> {
        let ClassBinding {
            class_name,
            documentation,
            fields,
            source,
        } = self;

        write_class(writer, class_name.clone(), documentation, fields.clone())?;
        // write_transposition(writer, class_name, fields)?;

        panic!();
        Ok(())
    }
}

fn write_class(
    writer: &mut JavaSourceWriter,
    class_name: String,
    documentation: Documentation,
    fields: Vec<JavaField>,
) -> io::Result<()> {
    let file_writer = writer.for_file(class_name.clone())?;
    let mut class_writer = file_writer.begin_class(class_name, documentation)?;

    let mut methods = Vec::with_capacity(fields.len());

    for field in &fields {
        methods.push(JavaMethod::getter_for(field.clone()));
        methods.push(JavaMethod::setter_for(field.clone()));

        let JavaField {
            name,
            documentation,
            ty,
            default_value,
        } = field;
        class_writer
            .field(*ty, default_value.clone())
            .set_documentation(documentation.content())
            .write(name)?;
    }

    for method in methods {
        class_writer.write_method(method)?;
    }

    class_writer.write_method(byte_transposition_method(&fields))?;
    class_writer.end_class()
}

fn byte_transposition_method(fields: &[JavaField]) -> JavaMethod {
    let mut method = JavaMethod::new(AS_BYTES_METHOD, JavaType::Array(PrimitiveJavaType::Byte))
        .add_documentation("Returns a byte array representation of the current configuration.")
        .add_documentation(
            "\nThis method is not intended for public use and is called by the Swim runtime.",
        );

    let mut transposition = Block::from(format!(
        "java.nio.ByteBuffer {} = java.nio.ByteBuffer.allocate({})",
        BUFFER_VAR, BUFFER_SIZE_VAR
    ))
    .add_statement(format!(
        "{}.order(java.nio.ByteOrder.LITTLE_ENDIAN)",
        BUFFER_VAR
    ));
    let mut block = Block::from(format!("int {} = 0", BUFFER_SIZE_VAR));
    let mut size = 0;

    for field in fields {
        let JavaField { name, ty, .. } = field;

        match ty {
            JavaType::Void => {
                unreachable!("Bug: field type set to void")
            }
            JavaType::Primitive(ty) => {
                size += ty.size_of();

                let stmt = match ty {
                    PrimitiveJavaType::Byte => {
                        format!("{}.put(this.{})", BUFFER_VAR, name)
                    }
                    PrimitiveJavaType::Int => {
                        format!("{}.putInt(this.{})", BUFFER_VAR, name)
                    }
                    PrimitiveJavaType::Long => {
                        format!("{}.putLong(this.{})", BUFFER_VAR, name)
                    }
                    PrimitiveJavaType::Float => {
                        format!("{}.putFloat(this.{})", BUFFER_VAR, name)
                    }
                    PrimitiveJavaType::Double => {
                        format!("{}.putDouble(this.{})", BUFFER_VAR, name)
                    }
                    PrimitiveJavaType::Boolean => {
                        format!("{}.put((byte) this.{} ? 1 : 0))", BUFFER_VAR, name)
                    }
                };
                transposition = transposition.add_statement(stmt);
            }
            JavaType::String => {
                block = block.add_statement(format!("{} += {}.length()", BUFFER_SIZE_VAR, name));
                transposition = transposition
                    .add_statement(format!("{}.putInt(this.{}.length())", BUFFER_VAR, name))
                    .add_statement(format!(
                        "{}.put(this.{}.getBytes(java.nio.charset.StandardCharsets.UTF_8))",
                        BUFFER_VAR, name
                    ));
            }
            JavaType::Array(ty) => {
                block = block.add_statement(format!(
                    "{} += ({}.length * {}",
                    BUFFER_SIZE_VAR,
                    name,
                    ty.size_of()
                ));
            }
        }
    }

    let body = block
        .add_statement(format!("{} += {}", BUFFER_SIZE_VAR, size))
        .extend(transposition.add_statement(format!("return {}.array()", BUFFER_VAR)));
    method.set_block(body)
}

#[derive(Debug)]
pub struct AbstractClassBinding {
    class_name: String,
    documentation: Documentation,
    variants: ClassBinding,
}

impl AbstractClassBinding {
    pub fn write(self, writer: &mut JavaSourceWriter) -> io::Result<()> {
        unimplemented!()
    }
}

#[derive(Debug, Clone)]
pub struct JavaField {
    name: String,
    documentation: Documentation,
    ty: JavaType,
    default_value: String,
}

impl JavaField {
    pub fn default_value(&self) -> String {
        self.default_value.clone()
    }
}

#[derive(Debug)]
pub struct JavaMethodParameter {
    name: String,
    ty: JavaType,
}

impl Display for JavaMethodParameter {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{} {}", self.ty, self.name)
    }
}

impl JavaMethodParameter {
    pub fn new(name: impl ToString, ty: JavaType) -> JavaMethodParameter {
        JavaMethodParameter {
            name: name.to_string(),
            ty,
        }
    }

    fn from_field(field: &JavaField) -> JavaMethodParameter {
        JavaMethodParameter {
            name: field.name.clone(),
            ty: field.ty.clone(),
        }
    }
}

pub struct FieldModifiers {
    r#static: bool,
    protected: bool,
    public: bool,
    r#final: bool,
}

impl Default for FieldModifiers {
    fn default() -> Self {
        FieldModifiers {
            r#static: false,
            protected: false,
            public: true,
            r#final: false,
        }
    }
}

impl FieldModifiers {
    fn set_static(mut self, to: bool) -> FieldModifiers {
        self.r#static = to;
        self
    }

    fn set_protected(mut self, to: bool) -> FieldModifiers {
        if self.public && to {
            panic!("Attempted to set an illegal combination of protected and public");
        }
        self.protected = to;
        self
    }

    fn set_public(mut self, to: bool) -> FieldModifiers {
        if self.protected && to {
            panic!("Attempted to set an illegal combination of protected and public");
        }
        self.public = to;
        self
    }

    fn set_final(mut self, to: bool) -> FieldModifiers {
        self.r#final = to;
        self
    }

    fn write(self, writer: &mut Writer) -> io::Result<()> {
        let FieldModifiers {
            r#static,
            protected,
            public,
            r#final,
        } = self;

        let mut modifiers = Vec::new();

        if public {
            modifiers.push("public");
        }
        if protected {
            modifiers.push("protected");
        }
        if r#static {
            modifiers.push("static");
        }
        if r#final {
            modifiers.push("final");
        }

        writer.write_indented(modifiers.join(" "), false)
    }
}

#[derive(Default)]
pub struct Block {
    lines: Vec<String>,
}

impl From<&str> for Block {
    fn from(s: &str) -> Self {
        let block = Block { lines: Vec::new() };
        block.add_statement(s)
    }
}

impl From<String> for Block {
    fn from(s: String) -> Self {
        Block::from(s.as_str())
    }
}

impl Block {
    pub fn write(self, writer: &mut Writer) -> io::Result<()> {
        writer.write_all_indented(self.lines.into_iter(), false)
    }

    pub fn add_statement(mut self, statement: impl ToString) -> Block {
        self.lines.push(format!("{};", statement.to_string()));
        self
    }

    pub fn extend(mut self, with: Block) -> Block {
        self.lines.extend(with.lines);
        self
    }

    pub fn push_line(mut self) -> Block {
        self.lines.push("\n".to_string());
        self
    }
}

pub struct JavaMethod {
    pub name: String,
    pub documentation: Documentation,
    pub return_type: JavaType,
    pub args: Vec<JavaMethodParameter>,
    pub body: Block,
    pub modifiers: FieldModifiers,
}

impl JavaMethod {
    pub fn new(name: impl ToString, return_type: JavaType) -> JavaMethod {
        JavaMethod {
            name: name.to_string(),
            documentation: Documentation::empty(),
            return_type,
            args: vec![],
            body: Block::default(),
            modifiers: FieldModifiers::default(),
        }
    }

    pub fn set_block(mut self, to: Block) -> JavaMethod {
        self.body = to;
        self
    }

    pub fn add_parameter(mut self, param: JavaMethodParameter) -> JavaMethod {
        self.args.push(param);
        self
    }

    pub fn add_documentation(mut self, line: impl ToString) -> JavaMethod {
        self.documentation.push_line(line.to_string());
        self
    }

    pub fn add_statement(mut self, statement: impl ToString) -> JavaMethod {
        self.body = self.body.add_statement(statement);
        self
    }

    pub fn getter_for(field: JavaField) -> JavaMethod {
        let documentation = Documentation::getter(field.name.clone())
            .set_default(field.default_value())
            .build();
        let body = Block::from(format!("return this.{}", field.name));

        JavaMethod {
            name: format!("get{}", AsUpperCamelCase(field.name)),
            documentation,
            return_type: field.ty,
            args: vec![],
            body,
            modifiers: FieldModifiers::default(),
        }
    }

    pub fn setter_for(field: JavaField) -> JavaMethod {
        let documentation = Documentation::setter(field.name.clone()).build();
        let body = Block::from(format!("this.{} = {}", field.name, field.name));
        let arg = JavaMethodParameter::from_field(&field);
        JavaMethod {
            name: format!("set{}", AsUpperCamelCase(field.name)),
            documentation,
            return_type: JavaType::Void,
            args: vec![arg],
            body,
            modifiers: FieldModifiers::default(),
        }
    }
}
