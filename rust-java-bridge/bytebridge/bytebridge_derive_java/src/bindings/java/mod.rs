use std::io;

use heck::AsLowerCamelCase;
use proc_macro2::Span;
use quote::ToTokens;
use syn::parse::{Parse, ParseStream};
use syn::punctuated::Punctuated;
use syn::spanned::Spanned;
use syn::visit::Visit;
use syn::{
    Attribute, Error, Expr, ExprLit, Field, ItemEnum, Lit, LitInt, Meta, MetaNameValue,
    PathSegment, Token, Type, TypePath, Variant,
};

pub use writer::{JavaSourceWriter, JavaSourceWriterBuilder};

use crate::bindings::java::models::{
    try_map_type, Block, Constraint, ConstraintKind, JavaField, JavaMethod, JavaType,
    PrimitiveJavaType, RustType, JAVA_KEYWORDS, PACKER_VAR, PACK_METHOD, RESERVED_VARIABLES,
    TEMP_VAR, TO_STRING_METHOD,
};
use crate::bindings::java::writer::{ClassType, INDENTATION};
use crate::bindings::{
    ATTR_DEFAULT_VALUE, ATTR_DOC, ATTR_NATURAL, ATTR_NON_ZERO, ATTR_RANGE, ATTR_RENAME,
    ATTR_UNSIGNED_ARRAY, INVALID_PROPERTY_NAME, MACRO_PATH, UNKNOWN_ATTRIBUTE,
};
use crate::docs::Documentation;
use crate::FormatStyle;

mod models;
mod writer;

const IO_EXCEPTION: &str = "IOException";
const OVERRIDE_ANNOTATION: &str = "@Override";

/// A derived class binding from a Rust struct.
#[derive(Debug)]
pub struct ClassBinding {
    /// The name of the class.
    name: String,
    /// Class-level documentation to apply.
    root_documentation: Documentation,
    /// A vector of fields.
    fields: Vec<JavaField>,
    /// A vector of instance or abstract methods.
    methods: Vec<JavaMethod>,
}

/// An abstract class configuration.
struct InheritanceConfig {
    /// The ordinal in the enumeration constant.
    ordinal: u8,
    /// The name of the superclass.
    superclass: String,
}

impl ClassBinding {
    /// Write this class binding into the provided java writer.
    /// # Arguments:
    /// - java_writer: The writer that this binding will be written into.
    /// - parent: An optional inheritance configuration if this class binding is a subclass.
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

        let byte_representation_method = ClassBinding::byte_representation_method(
            &fields,
            parent.as_ref().map(|cfg| cfg.ordinal),
        );
        let to_string_method = ClassBinding::to_string_method(&name, &fields);

        let file_writer = java_writer.for_file(name.clone())?;
        let class_type = match parent.as_ref() {
            Some(cfg) => ClassType::Subclass(cfg.superclass.to_string()),
            None => ClassType::Concrete,
        };
        let mut class_writer = file_writer.begin_class(name, root_documentation, class_type)?;

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

        class_writer.write_method(byte_representation_method)?;
        class_writer.write_method(to_string_method)?;
        class_writer.end_class()
    }

    /// Derive a byte-level representation from a slice of java fields and an optional ordinal
    /// of an enumeration constant.
    fn byte_representation_method(fields: &[JavaField], ordinal: Option<u8>) -> JavaMethod {
        let method = JavaMethod::new(
            PACK_METHOD,
            JavaType::Void,
            ordinal.as_ref().map(|_| OVERRIDE_ANNOTATION.to_string()),
        )
        .add_documentation("Returns a byte array representation of the current configuration.")
        .add_arg(PACKER_VAR, JavaType::Object("MessagePacker".to_string()))
        .add_throws(IO_EXCEPTION);

        let mut byte_representation = match ordinal {
            Some(idx) => Block::of_statement(format!(
                "{}.packExtensionTypeHeader((byte) 1, 1)",
                PACKER_VAR
            ))
            .add_statement(format!("{}.packInt({idx})", PACKER_VAR)),
            None => Block::default(),
        };

        byte_representation = byte_representation.add_statement(format!(
            "{}.packArrayHeader({})",
            PACKER_VAR,
            fields.len()
        ));

        for field in fields {
            let JavaField { name, ty, .. } = field;

            match ty {
                JavaType::Void => {
                    unreachable!("Bug: field type set to void")
                }
                JavaType::Primitive(ty) => {
                    let name = format!("this.{name}");
                    byte_representation =
                        byte_representation.add_statement(put_primitive(ty, &name));
                }
                JavaType::String => {
                    byte_representation = byte_representation
                        .add_statement(format!("{}.packString(this.{})", PACKER_VAR, name));
                }
                JavaType::Array(ty) => {
                    byte_representation = byte_representation
                        .add_statement(format!("{}.packInt(this.{}.length)", PACKER_VAR, name))
                        .add_line(format!("for ({} {} : this.{}) {{", ty, TEMP_VAR, name))
                        .add_statement(format!("{INDENTATION}{}", put_primitive(ty, TEMP_VAR)))
                        .add_line("}");
                }
                JavaType::Object(_) => {
                    byte_representation =
                        byte_representation.add_statement(format!("{}.pack({})", name, PACKER_VAR));
                }
            }
        }

        method.set_block(byte_representation)
    }

    fn to_string_method(name: &str, fields: &[JavaField]) -> JavaMethod {
        let method = JavaMethod::new(
            TO_STRING_METHOD,
            JavaType::String,
            Some(OVERRIDE_ANNOTATION.to_string()),
        );

        if fields.is_empty() {
            return method.set_block(Block::of(format!("\"{}{{}}\"", name)));
        }

        let mut block = Block::of(format!("return \"{}{{\" +\n", name));
        let mut first = true;

        for field in fields.iter() {
            let prefix = if first {
                first = false;
                "\""
            } else {
                "\", "
            };
            let name = &field.name;

            block = block.add(format!("{prefix}{name}='\" + {name} + '\\'' +\n"))
        }

        method.set_block(block.add_statement(" '}'"))
    }
}

/// Resolves an instance method call on a Java ByteBuffer that corresponds to a primitive java type.
fn put_primitive(ty: &PrimitiveJavaType, name: &str) -> String {
    match ty {
        PrimitiveJavaType::Byte { .. } => {
            format!("{}.packByte({})", PACKER_VAR, name)
        }
        PrimitiveJavaType::Int { .. } => {
            format!("{}.packInt({})", PACKER_VAR, name)
        }
        PrimitiveJavaType::Long { .. } => {
            format!("{}.packLong({})", PACKER_VAR, name)
        }
        PrimitiveJavaType::Float => {
            format!("{}.packFloat({})", PACKER_VAR, name)
        }
        PrimitiveJavaType::Double => {
            format!("{}.packDouble({})", PACKER_VAR, name)
        }
        PrimitiveJavaType::Boolean => {
            format!("{}.packBoolean({})", PACKER_VAR, name)
        }
        PrimitiveJavaType::Short { .. } => {
            format!("{}.packShort({})", PACKER_VAR, name)
        }
        PrimitiveJavaType::Char => {
            format!("{}.packString(String.valueOf({}))", PACKER_VAR, name)
        }
    }
}

/// A derived polymorphic class binding from a Rust enumeration.
#[derive(Debug)]
pub struct AbstractClassBinding {
    /// The name of the parent class.
    name: String,
    /// Class-level documentation to apply.
    documentation: Documentation,
    /// A vector of subclasses.
    variants: Vec<ClassBinding>,
}

impl AbstractClassBinding {
    /// Write this class binding into the provided Java writer.
    /// # Arguments:
    /// - java_writer: The writer that this binding will be written into.
    fn write(self, java_writer: &mut JavaSourceWriter) -> io::Result<()> {
        let AbstractClassBinding {
            name,
            documentation,
            variants,
        } = self;

        let file_writer = java_writer.for_file(name.clone())?;
        let mut class_writer =
            file_writer.begin_class(name.clone(), documentation, ClassType::Abstract)?;

        let pack_method = JavaMethod::new(PACK_METHOD, JavaType::Void, None)
            .add_documentation("Pack a byte representation of the current configuration.")
            .add_arg(PACKER_VAR, JavaType::Object("MessagePacker".to_string()))
            .add_throws(IO_EXCEPTION)
            .set_abstract();

        let to_string_method = JavaMethod::new(
            TO_STRING_METHOD,
            JavaType::String,
            Some(OVERRIDE_ANNOTATION.to_string()),
        )
        .set_abstract();

        class_writer.write_method(to_string_method)?;
        class_writer.write_method(pack_method)?;
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

/// A Syn Visitor for deriving a class from a Syn item struct.
pub struct ClassBuilder {
    /// Whether to infer Java documentation from Rust documentation attributes (#[doc = "..."]).
    infer_docs: bool,
    /// The current state of the visit.
    result: Result<ClassBuilderInner, Error>,
}

impl ClassBuilder {
    /// Returns a new class builder.
    /// # Arguments:
    /// - infer_docs: Whether to infer Java documentation from Rust documentation attributes
    ///   (#[doc = "..."]).
    /// - root_documentation: Root-level documentation that will be applied to the class when it is
    ///   written.
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

    /// Executes 'f' against the current state if it is in a Result::Ok state.
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

/// A Syn Visitor for deriving a polymorphic class from a Syn item enum.
pub struct AbstractClassBuilder {
    /// Whether to infer Java documentation from Rust documentation attributes (#[doc = "..."]).
    infer_docs: bool,
    /// The name of the parent class.
    name: String,
    /// Root-level documentation that will be applied to the class when it is written.
    root_documentation: Documentation,
    /// The current state of the visit.
    inner: Result<Vec<ClassBinding>, Error>,
}

impl AbstractClassBuilder {
    /// Returns a new polymorphic class builder.
    /// # Arguments:
    /// - infer_docs: Whether to infer Java documentation from Rust documentation attributes
    ///   (#[doc = "..."]).
    /// - root_documentation: Root-level documentation that will be applied to the class when it is
    ///   written.
    /// - name: The name of the parent class.
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

    /// Returns a subclass builder
    /// # Arguments:
    /// - name: The name of the subclass.
    /// - root_documentation: Root-level documentation that will be applied to the class when it is
    ///   written.
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

    /// Attempts to push a subclass.
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

impl<'ast> Visit<'ast> for AbstractClassBuilder {
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

/// Attempts to build properties for a variant. This will fail if any attributes are malformed.
///
/// # Arguments:
/// - enum_name: The name of the parent class.
///   (#[doc = "..."]).
/// - variant_name: The name of the subclass.
/// - attrs: A slice of attributes that have been decorated on the variant.
fn build_variant_properties(
    enum_name: &String,
    variant_name: String,
    attrs: &Vec<Attribute>,
) -> Result<VariantProperties, Error> {
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
                Meta::NameValue(meta) if meta.path.is_ident(ATTR_DOC) => match meta.value {
                    Expr::Lit(ExprLit {
                        lit: Lit::Str(str), ..
                    }) => properties.documentation.push_header_line(str.value()),
                    list => return Err(Error::new(list.span(), UNKNOWN_ATTRIBUTE)),
                },
                Meta::NameValue(meta) if meta.path.is_ident(ATTR_RENAME) => {
                    properties.subclass_name = validate_identifier(meta)?
                }
                meta => return Err(Error::new(meta.span(), UNKNOWN_ATTRIBUTE)),
            }
        }
    }

    Ok(properties)
}

fn ident_start_char(c: char) -> bool {
    c.is_alphabetic() || c == '$' || c == '_'
}

/// Validates a literal that has been applied to a meta name value. This will fail if the literal is
/// not a string or if the string value is an invalid Java identifier.
pub fn validate_identifier(meta: MetaNameValue) -> Result<String, Error> {
    let err = |span, msg| Err(Error::new(span, msg));

    match meta.value {
        Expr::Lit(ExprLit {
            lit: Lit::Str(str), ..
        }) => {
            let identifier = str.value();
            let mut chars = identifier.chars();
            match chars.next() {
                Some(char) if ident_start_char(char) => {
                    if chars.all(|c| c.is_numeric() || ident_start_char(c)) {
                        Ok(identifier)
                    } else {
                        err(str.span(), INVALID_PROPERTY_NAME)
                    }
                }
                Some(_) => err(str.span(), INVALID_PROPERTY_NAME),
                None => err(str.span(), INVALID_PROPERTY_NAME),
            }
        }
        list => err(list.span(), INVALID_PROPERTY_NAME),
    }
}

impl<'ast> Visit<'ast> for ClassBuilder {
    fn visit_field(&mut self, field: &'ast Field) {
        self.with(|infer_docs, builder| {
            let (java_ty, rust_ty) = map_type(&field.ty)?;
            let span = field.span();
            let name = field
                .ident
                .as_ref()
                .ok_or_else(|| Error::new(span, "Tuple fields are not supported"))
                .map(|i| AsLowerCamelCase(i.to_string()).to_string())?;
            let (name, properties) = derive_field_properties(
                name,
                field.span(),
                infer_docs,
                java_ty.clone(),
                rust_ty,
                &field.attrs,
            )?;
            validate_name(name.as_str(), span)?;

            let field = JavaField {
                name,
                documentation: properties.documentation,
                default_value: properties
                    .default_value
                    .unwrap_or_else(|| java_ty.default_value()),
                ty: java_ty,
                constraint: properties.constraint,
            };

            builder.push_method(JavaMethod::getter_for(field.clone()));
            builder.push_method(JavaMethod::setter_for(field.clone()));
            builder.push_field(field);

            Ok(())
        });
    }
}

/// Derived field properties from a field's attributes.
struct FieldProperties {
    /// Documentation that will either override a field's Rust documentation if 'no_doc' has been
    /// applied at a container level or documentation that will be extended to it.
    documentation: Documentation,
    /// The field's default value.
    default_value: Option<String>,
    /// A constraint that has been applied to the field.
    constraint: Constraint,
}

/// Attempts to align a Rust type to a Java Type.
pub fn map_type(ty: &Type) -> Result<(JavaType, RustType), Error> {
    let unsupported_type = |span| Err(Error::new(span, "Unsupported type"));
    match ty {
        Type::Path(TypePath { qself: None, path }) => match path.segments.last() {
            Some(PathSegment { ident, arguments }) => {
                match try_map_type(ident.to_string().as_str(), arguments) {
                    Ok(tys) => Ok(tys),
                    Err(e) => Err(Error::new(path.span(), e)),
                }
            }
            None => unsupported_type(Span::call_site()),
        },
        meta => unsupported_type(meta.span()),
    }
}

/// Infers documentation from the Rust attribute #[doc = "..."] and writes it into 'documentation'.
fn infer_docs(attrs: &[Attribute], documentation: &mut Documentation) -> Result<(), Error> {
    let attrs = attrs.iter().filter(|at| at.path().is_ident(ATTR_DOC));
    for attr in attrs {
        match &attr.meta {
            Meta::NameValue(meta) => match &meta.value {
                Expr::Lit(ExprLit {
                    lit: Lit::Str(str), ..
                }) => documentation.push_header_line(str.value()),
                v => {
                    return Err(Error::new_spanned(
                        v,
                        format!("Invalid documentation type: {}", v.to_token_stream()),
                    ))
                }
            },
            _ => {}
        }
    }

    Ok(())
}

/// Derives field properties from a slice of attributes. Returns a new name for the field if a valid
/// rename attribute has been applied, and field properties.
///
/// # Arguments:
/// - name: The default name of the field. If a valid rename attribute has been applied then the new
///   name will be returned.
/// - infer_documentation: Whether to infer documentation for the field from Rust documentation
///   attributes.
/// - field_type: The Java type of the field.
/// - attrs: a slice of attributes to validate.
fn derive_field_properties(
    mut name: String,
    span: Span,
    infer_documentation: bool,
    java_type: JavaType,
    rust_type: RustType,
    attrs: &[Attribute],
) -> Result<(String, FieldProperties), Error> {
    let mut properties = FieldProperties {
        documentation: Documentation::from_style(FormatStyle::Documentation),
        default_value: None,
        constraint: Constraint::new(span, ConstraintKind::None),
    };

    if infer_documentation {
        infer_docs(attrs, &mut properties.documentation)?;
    }

    let mut attribute_iter = attrs
        .iter()
        .filter(|at| at.path().is_ident(MACRO_PATH))
        .peekable();

    if attribute_iter.peek().is_none() {
        Constraint::implicit(&mut properties.constraint, &java_type)?;
        return Ok((name, properties));
    }

    let unknown_attribute =
        |attr, span| Err(Error::new(span, format!("Unknown attribute: {}", attr)));

    let mut documentation_override = Documentation::empty_documentation();

    for attr in attribute_iter {
        let nested = attr.parse_args_with(Punctuated::<Meta, Token![,]>::parse_terminated)?;
        for nested in nested {
            match nested {
                Meta::NameValue(meta) if meta.path.is_ident(ATTR_DOC) => match meta.value {
                    Expr::Lit(ExprLit {
                        lit: Lit::Str(str), ..
                    }) => documentation_override.push_header_line(str.value()),
                    meta => {
                        return unknown_attribute(meta.to_token_stream().to_string(), meta.span())
                    }
                },
                Meta::NameValue(meta) if meta.path.is_ident(ATTR_RENAME) => {
                    name = validate_identifier(meta)?;
                }
                Meta::NameValue(MetaNameValue {
                    path,
                    value: Expr::Lit(ExprLit { lit, .. }),
                    ..
                }) if path.is_ident(ATTR_DEFAULT_VALUE) => {
                    if properties.default_value.is_some() {
                        return Err(Error::new(lit.span(), "Duplicate default value"));
                    }
                    properties.default_value =
                        Some(java_type.unpack_default_value(lit, rust_type)?);
                }
                Meta::Path(path) if path.is_ident(ATTR_NON_ZERO) => properties
                    .constraint
                    .step(java_type.as_non_zero(path.span())?)?,
                Meta::Path(path) if path.is_ident(ATTR_NATURAL) => properties
                    .constraint
                    .step(java_type.as_natural(path.span())?)?,
                Meta::Path(path) if path.is_ident(ATTR_UNSIGNED_ARRAY) => properties
                    .constraint
                    .step(java_type.as_unsigned_array(path.span())?)?,
                Meta::List(list) if list.path.is_ident(ATTR_RANGE) => {
                    struct RangeArgs {
                        min: LitInt,
                        _sep: Token![,],
                        max: LitInt,
                    }

                    impl Parse for RangeArgs {
                        fn parse(input: ParseStream) -> syn::Result<Self> {
                            Ok(RangeArgs {
                                min: input.parse()?,
                                _sep: input.parse()?,
                                max: input.parse()?,
                            })
                        }
                    }

                    let args = list.parse_args::<RangeArgs>()?;
                    properties.constraint.step(java_type.as_range(
                        list.span(),
                        args.min,
                        args.max,
                    )?)?
                }
                meta => {
                    return unknown_attribute(meta.to_token_stream().to_string(), meta.span());
                }
            }
        }
    }

    Constraint::implicit(&mut properties.constraint, &java_type)?;

    if !documentation_override.is_empty() {
        properties.documentation = documentation_override;
    }

    Ok((name, properties))
}

/// Sanitizes 'name' to ensure that it is not a reserved Java or bytebridge keyword.
fn validate_name(name: &str, span: Span) -> Result<(), Error> {
    if JAVA_KEYWORDS.contains(name) {
        Err(Error::new(
            span,
            format!(
                "Attempted to use a reserved keyword as a field name: {}",
                name
            ),
        ))
    } else if RESERVED_VARIABLES.contains(&name) {
        Err(Error::new(
            span,
            format!(
                "Attempted to use a reserved variable name as a field name: {}",
                name
            ),
        ))
    } else {
        Ok(())
    }
}
