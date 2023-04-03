mod models;
mod writer;

use crate::bindings::java::models::{
    Block, Constraint, JavaField, JavaMethod, JavaType, PrimitiveJavaType, AS_BYTES_METHOD,
    BUFFER_SIZE_VAR, BUFFER_VAR, JAVA_KEYWORDS,
};
use crate::bindings::MACRO_PATH;
use crate::docs::Documentation;
use crate::FormatStyle;
use proc_macro2::Span;
use quote::ToTokens;
use std::io;
use std::mem::size_of;
use syn::punctuated::Punctuated;
use syn::spanned::Spanned;
use syn::visit::Visit;
use syn::{
    Attribute, Error, Expr, ExprLit, Field, ItemEnum, Lit, Meta, MetaNameValue, PathArguments,
    PathSegment, Token, Type, TypePath, Variant,
};

pub use writer::{JavaSourceWriter, JavaSourceWriterBuilder};

#[derive(Debug)]
pub struct ClassBinding {
    name: String,
    root_documentation: Documentation,
    fields: Vec<JavaField>,
    methods: Vec<JavaMethod>,
}

struct InheritanceConfig {
    ordinal: u8,
    superclass: String,
}

impl ClassBinding {
    fn write(
        self,
        java_writer: &mut JavaSourceWriter,
        parent: Option<InheritanceConfig>,
    ) -> io::Result<()> {
        let ClassBinding {
            name,
            root_documentation,
            fields,
            methods,
        } = self;

        let transposition_method = ClassBinding::byte_transposition_method(
            &fields,
            parent.as_ref().map(|cfg| cfg.ordinal),
        );

        let file_writer = java_writer.for_file(name.clone())?;
        let mut class_writer = file_writer.begin_class(
            name,
            root_documentation,
            parent.as_ref().map(|cfg| cfg.superclass.clone()),
        )?;

        for field in fields {
            let JavaField {
                name,
                documentation,
                ty,
                default_value,
                ..
            } = field;
            class_writer
                .field(ty, default_value)
                .set_documentation(documentation)
                .write(name)?;
        }

        for method in methods {
            class_writer.write_method(method)?;
        }

        class_writer.write_method(transposition_method)?;
        class_writer.end_class()
    }

    fn byte_transposition_method(fields: &[JavaField], ordinal: Option<u8>) -> JavaMethod {
        let method = JavaMethod::new(AS_BYTES_METHOD, JavaType::Array(PrimitiveJavaType::Byte))
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
        let mut size = match ordinal {
            Some(i) => {
                block = block.add_statement(format!("{}.put({})", BUFFER_VAR, i));
                size_of::<u8>()
            }
            None => 0,
        };

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
                    block =
                        block.add_statement(format!("{} += {}.length()", BUFFER_SIZE_VAR, name));
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
}

#[derive(Debug)]
pub struct AbstractClassBinding {
    name: String,
    documentation: Documentation,
    variants: Vec<ClassBinding>,
}

impl AbstractClassBinding {
    fn write(self, java_writer: &mut JavaSourceWriter) -> io::Result<()> {
        let AbstractClassBinding {
            name,
            documentation,
            variants,
        } = self;

        let file_writer = java_writer.for_file(name.clone())?;
        let mut class_writer = file_writer.begin_class(name.clone(), documentation, None)?;

        let method = JavaMethod::new(AS_BYTES_METHOD, JavaType::Array(PrimitiveJavaType::Byte))
            .add_documentation("Returns a byte array representation of the current configuration.")
            .add_documentation(
                "\nThis method is not intended for public use and is called by the Swim runtime.",
            );
        let mut block = Block::default();

        let mut variant_iter = variants.iter().peekable();
        let first_variant = variant_iter
            .next()
            .expect("Bug: An enum should contain at least one variant");

        block = block.add(format!("if (this instanceof {}) {{", first_variant.name));
        block = block.add(format!(
            "\treturn (({}) this).asBytes();\n\t}}",
            first_variant.name,
        ));

        for variant in variant_iter {
            block = block.add(format!("else if (this instanceof {}) {{", variant.name));
            block = block.add(format!(
                "\treturn (({}) this).asBytes();\n\t}}",
                variant.name,
            ));
        }

        class_writer.write_method(method.set_block(block))?;
        class_writer.end_class()?;

        // We defer writing the variants here in case the file writer is writing to STDOUT. We want
        // to write the abstract base first, then the subclasses.
        for (idx, variant) in variants.into_iter().enumerate() {
            variant.write(
                java_writer,
                Some(InheritanceConfig {
                    ordinal: u8::try_from(idx).expect("Bug: too many variants"),
                    superclass: name.clone(),
                }),
            )?;
        }

        Ok(())
    }
}

#[derive(Debug)]
pub enum JavaBindings {
    Class(ClassBinding),
    Abstract(AbstractClassBinding),
}

impl JavaBindings {
    pub fn write(self, java_writer: &mut JavaSourceWriter) -> io::Result<()> {
        match self {
            JavaBindings::Class(bindings) => bindings.write(java_writer, None),
            JavaBindings::Abstract(bindings) => bindings.write(java_writer),
        }
    }
}

pub struct ClassBuilder {
    infer_docs: bool,
    result: Result<ClassBuilderInner, Error>,
}

impl ClassBuilder {
    pub fn new(
        infer_docs: bool,
        root_documentation: Documentation,
        name: impl ToString,
    ) -> ClassBuilder {
        ClassBuilder {
            infer_docs,
            result: Ok(ClassBuilderInner::new(name, root_documentation)),
        }
    }

    fn with<F>(&mut self, f: F)
    where
        F: FnOnce(bool, &mut ClassBuilderInner) -> Result<(), Error>,
    {
        let ClassBuilder { infer_docs, result } = self;
        if let Ok(builder) = result {
            if let Err(e) = f(*infer_docs, builder) {
                *result = Err(e);
            }
        }
    }

    pub fn into_result(self) -> Result<ClassBinding, Error> {
        self.result.map(ClassBuilderInner::build)
    }
}

#[derive(Debug)]
struct ClassBuilderInner {
    name: String,
    root_documentation: Documentation,
    fields: Vec<JavaField>,
    methods: Vec<JavaMethod>,
}

impl ClassBuilderInner {
    pub fn new(name: impl ToString, root_documentation: Documentation) -> ClassBuilderInner {
        ClassBuilderInner {
            name: name.to_string(),
            root_documentation,
            fields: vec![],
            methods: vec![],
        }
    }

    fn push_field(&mut self, field: JavaField) {
        self.fields.push(field);
    }

    fn push_method(&mut self, method: JavaMethod) {
        self.methods.push(method);
    }

    fn build(self) -> ClassBinding {
        let ClassBuilderInner {
            name,
            root_documentation,
            fields,
            methods,
        } = self;
        ClassBinding {
            name,
            root_documentation,
            fields,
            methods,
        }
    }
}

pub struct AbstractClassBuilder {
    infer_docs: bool,
    name: String,
    root_documentation: Documentation,
    inner: Result<Vec<ClassBinding>, Error>,
}

impl AbstractClassBuilder {
    pub fn new(
        infer_docs: bool,
        root_documentation: Documentation,
        name: impl ToString,
    ) -> AbstractClassBuilder {
        AbstractClassBuilder {
            infer_docs,
            name: name.to_string(),
            root_documentation,
            inner: Ok(vec![]),
        }
    }

    pub fn class_builder(
        &mut self,
        name: impl ToString,
        root_documentation: Documentation,
    ) -> ClassBuilder {
        ClassBuilder {
            infer_docs: self.infer_docs,
            result: Ok(ClassBuilderInner {
                name: name.to_string(),
                root_documentation,
                fields: vec![],
                methods: vec![],
            }),
        }
    }

    fn push_class(&mut self, class_builder: ClassBuilderInner) {
        if let Ok(variants) = &mut self.inner {
            variants.push(class_builder.build())
        }
    }

    pub fn into_result(self) -> Result<AbstractClassBinding, Error> {
        let AbstractClassBuilder {
            name,
            root_documentation,
            inner,
            ..
        } = self;
        match inner {
            Ok(variants) => {
                if variants.is_empty() {
                    return Err(Error::new(Span::call_site(), "Enum contains no variants"));
                }

                Ok(AbstractClassBinding {
                    name,
                    variants,
                    documentation: root_documentation,
                })
            }
            Err(e) => Err(e),
        }
    }
}

impl<'args, 'ast> Visit<'ast> for AbstractClassBuilder {
    fn visit_item_enum(&mut self, i: &'ast ItemEnum) {
        let lim = u8::MAX as usize;
        if i.variants.len() > lim {
            self.inner = Err(Error::new_spanned(
                &i.variants,
                format!("Enum variant limit is {}", lim),
            ));
        } else {
            for variant in &i.variants {
                self.visit_variant(variant);
            }
        }
    }

    fn visit_variant(&mut self, i: &'ast Variant) {
        if self.inner.is_ok() {
            match build_variant_properties(&self.name, i.ident.to_string(), &i.attrs) {
                Ok(properties) => {
                    let VariantProperties {
                        documentation,
                        subclass_name,
                    } = properties;

                    let mut class_builder = self.class_builder(subclass_name, documentation);
                    class_builder.visit_fields(&i.fields);

                    match class_builder.result {
                        Ok(inner) => {
                            self.push_class(inner);
                        }
                        Err(e) => {
                            self.inner = Err(e);
                        }
                    }
                }
                Err(e) => self.inner = Err(e),
            }
        }
    }
}

struct VariantProperties {
    documentation: Documentation,
    subclass_name: String,
}

fn build_variant_properties(
    enum_name: &String,
    variant_name: String,
    attrs: &Vec<Attribute>,
) -> Result<VariantProperties, Error> {
    const UNKNOWN_ATTRIBUTE: &str = "Unknown attribute";
    const INVALID_SUBCLASS: &str = "Invalid subclass name";

    let err = |span, msg| Err(Error::new(span, msg));

    let mut properties = VariantProperties {
        documentation: Documentation::from_style(FormatStyle::Documentation),
        subclass_name: format!("{}{}", enum_name, variant_name),
    };

    for attr in attrs {
        if !attr.path().is_ident(MACRO_PATH) {
            continue;
        }

        let nested = attr.parse_args_with(Punctuated::<Meta, Token![,]>::parse_terminated)?;
        for nested in nested {
            match nested {
                Meta::NameValue(meta) if meta.path.is_ident("doc") => match meta.value {
                    Expr::Lit(ExprLit {
                        lit: Lit::Str(str), ..
                    }) => properties.documentation.push_header_line(str.value()),
                    list => return Err(Error::new(list.span(), UNKNOWN_ATTRIBUTE)),
                },
                Meta::NameValue(meta) if meta.path.is_ident("rename") => match meta.value {
                    Expr::Lit(ExprLit {
                        lit: Lit::Str(str), ..
                    }) => {
                        let subclass_name = str.value();
                        let mut chars = subclass_name.chars();
                        match chars.next() {
                            Some(char) if char.is_alphabetic() => {
                                if chars.all(char::is_alphanumeric) {
                                    properties.subclass_name = subclass_name;
                                } else {
                                    return err(str.span(), INVALID_SUBCLASS);
                                }
                            }
                            Some(_) => return err(str.span(), INVALID_SUBCLASS),
                            None => return err(str.span(), INVALID_SUBCLASS),
                        }
                    }
                    list => return err(list.span(), INVALID_SUBCLASS),
                },
                meta => return Err(Error::new(meta.span(), UNKNOWN_ATTRIBUTE)),
            }
        }
    }

    Ok(properties)
}

impl<'args, 'ast> Visit<'ast> for ClassBuilder {
    fn visit_field(&mut self, field: &'ast Field) {
        self.with(|infer_docs, builder| {
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

            let field = JavaField {
                name,
                documentation: properties.documentation,
                ty,
                default_value,
                constraint: properties.constraint,
            };

            builder.push_method(JavaMethod::getter_for(field.clone()));
            builder.push_method(JavaMethod::setter_for(field.clone()));
            builder.push_field(field);

            Ok(())
        });
    }
}

struct FieldProperties {
    documentation: Documentation,
    default_value: Option<String>,
    constraint: Constraint,
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

fn derive_field_properties(
    infer_docs: bool,
    field_type: JavaType,
    attrs: &[Attribute],
) -> Result<FieldProperties, Error> {
    let mut properties = FieldProperties {
        documentation: Documentation::from_style(FormatStyle::Documentation),
        default_value: None,
        constraint: Constraint::None,
    };

    if !infer_docs {
        return Ok(properties);
    }

    let mut attribute_iter = attrs
        .iter()
        .filter(|at| at.path().is_ident(MACRO_PATH))
        .peekable();

    if attribute_iter.peek().is_none() {
        return Ok(properties);
    }

    let unknown_attribute = |span| Err(Error::new(span, "Unknown attribute"));

    for attr in attribute_iter {
        let nested = attr.parse_args_with(Punctuated::<Meta, Token![,]>::parse_terminated)?;

        for nested in nested {
            match nested {
                Meta::NameValue(meta) if meta.path.is_ident("doc") => match meta.value {
                    Expr::Lit(ExprLit {
                        lit: Lit::Str(str), ..
                    }) => properties.documentation.push_header_line(str.value()),
                    meta => return unknown_attribute(meta.span()),
                },
                Meta::NameValue(MetaNameValue {
                    path,
                    value: Expr::Lit(ExprLit { lit, .. }),
                    ..
                }) if path.is_ident("default_value") => {
                    if properties.default_value.is_some() {
                        return Err(Error::new(lit.span(), "Duplicate default value"));
                    }
                    properties.default_value = Some(field_type.unpack_default_value(lit)?);
                }
                Meta::Path(path) if path.is_ident("non_zero") => {
                    properties.constraint = field_type.as_non_zero(path.span())?
                }
                Meta::List(list) if list.path.is_ident("range") => {
                    println!("range: {}", list.to_token_stream().to_string());
                    unimplemented!();

                    // properties.constraint = field_type.as_non_zero(path.span())?
                }
                meta => {
                    return unknown_attribute(meta.span());
                }
            }
        }
    }

    Ok(properties)
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
