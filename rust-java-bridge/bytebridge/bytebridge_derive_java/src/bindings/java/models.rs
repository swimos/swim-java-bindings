use crate::bindings::java::writer::Writer;
use crate::bindings::java::writer::INDENTATION;
use crate::docs::Documentation;
use heck::AsUpperCamelCase;
use lazy_static::lazy_static;
use proc_macro2::Span;
use quote::ToTokens;
use std::collections::HashSet;
use std::fmt::{Display, Formatter};
use std::io;
use std::str::FromStr;
use syn::{Error, Lit, LitInt, PathArguments};

pub const BUFFER_SIZE_VAR: &str = "__buf__size";
pub const BUFFER_VAR: &str = "__buf";
pub const TEMP_VAR: &str = "__elem_";
pub const AS_BYTES_METHOD: &str = "asBytes";

lazy_static! {
    pub static ref JAVA_KEYWORDS: HashSet<&'static str> = {
        let set = HashSet::from([
            "abstract",
            "assert",
            "boolean",
            "break",
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
            TEMP_VAR,
        ]);
        set
    };
}

/// Discriminate between primitive Java types and arrays.
#[derive(Debug, Copy, Clone, PartialEq)]
pub enum JavaType {
    Void,
    String,
    Primitive(PrimitiveJavaType),
    Array(PrimitiveJavaType),
}

impl JavaType {
    pub fn try_map(ident: &str, arguments: &PathArguments) -> Result<JavaType, UnsupportedType> {
        match ident {
            "i8" => Ok(JavaType::Primitive(PrimitiveJavaType::Byte(false))),
            "i32" => Ok(JavaType::Primitive(PrimitiveJavaType::Int(false))),
            "i64" => Ok(JavaType::Primitive(PrimitiveJavaType::Long(false))),
            "u8" => Ok(JavaType::Primitive(PrimitiveJavaType::Byte(true))),
            "u32" => Ok(JavaType::Primitive(PrimitiveJavaType::Int(true))),
            "u64" => Ok(JavaType::Primitive(PrimitiveJavaType::Long(true))),
            "f32" => Ok(JavaType::Primitive(PrimitiveJavaType::Float)),
            "f64" => Ok(JavaType::Primitive(PrimitiveJavaType::Double)),
            "Duration" => Ok(JavaType::Primitive(PrimitiveJavaType::Int(true))),
            "String" => Ok(JavaType::String),
            "bool" => Ok(JavaType::Primitive(PrimitiveJavaType::Boolean)),
            "Vec" => {
                let err = || {
                    Err(UnsupportedType(format!(
                        "Vec{}",
                        arguments.to_token_stream().to_string()
                    )))
                };
                match arguments {
                    PathArguments::AngleBracketed(ang) => {
                        let mut iter = ang.args.pairs();
                        match (iter.next(), iter.next()) {
                            (Some(pair), None) => {
                                let value = pair.value();
                                match JavaType::try_map(
                                    value.to_token_stream().to_string().as_str(),
                                    &PathArguments::None,
                                ) {
                                    Ok(JavaType::Primitive(ty)) => Ok(JavaType::Array(ty)),
                                    _ => err(),
                                }
                            }
                            (Some(_), Some(_)) => err(),
                            (None, Some(_)) => unreachable!(),
                            (None, None) => err(),
                        }
                    }
                    _ => err(),
                }
            }
            v => Err(UnsupportedType(v.to_string())),
        }
    }
}

impl Display for JavaType {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            JavaType::Primitive(ty) => ty.fmt(f),
            JavaType::Array(ty) => {
                write!(f, "{}[]", ty)
            }
            JavaType::Void => {
                write!(f, "void")
            }
            JavaType::String => {
                write!(f, "String")
            }
        }
    }
}

#[derive(Debug, Copy, Clone, PartialEq)]
pub enum PrimitiveJavaType {
    Byte(bool),
    Int(bool),
    Long(bool),
    Float,
    Double,
    Boolean,
}

impl PrimitiveJavaType {
    pub fn int_like(&self) -> bool {
        matches!(
            self,
            PrimitiveJavaType::Int(_) | PrimitiveJavaType::Long(_) | PrimitiveJavaType::Byte(_)
        )
    }

    pub fn size_of(&self) -> usize {
        match self {
            PrimitiveJavaType::Byte(_) => 1,
            PrimitiveJavaType::Int(_) => 4,
            PrimitiveJavaType::Long(_) => 8,
            PrimitiveJavaType::Float => 4,
            PrimitiveJavaType::Double => 8,
            PrimitiveJavaType::Boolean => 1,
        }
    }
}

impl Display for PrimitiveJavaType {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            PrimitiveJavaType::Byte(_) => {
                write!(f, "byte")
            }
            PrimitiveJavaType::Int(_) => {
                write!(f, "int")
            }
            PrimitiveJavaType::Long(_) => {
                write!(f, "long")
            }
            PrimitiveJavaType::Float => {
                write!(f, "float")
            }
            PrimitiveJavaType::Double => {
                write!(f, "double")
            }
            PrimitiveJavaType::Boolean => {
                write!(f, "boolean")
            }
        }
    }
}

impl JavaType {
    pub fn default_value(&self) -> String {
        match self {
            JavaType::Primitive(PrimitiveJavaType::Byte(_)) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Int(_)) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Long(_)) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Float) => "0.0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Double) => "0.0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Boolean) => "false".to_string(),
            JavaType::Void => "".to_string(),
            JavaType::String => "\"\"".to_string(),
            JavaType::Array(arr) => {
                format!("new {}[] {{}}", arr)
            }
        }
    }

    pub fn as_unsigned_array(&self, span: Span) -> Result<Constraint, Error> {
        match self {
            JavaType::Array(ty) if ty.int_like() => {
                Ok(Constraint::new(span, ConstraintKind::UnsignedArray))
            }
            ty => Err(Error::new(
                span,
                format!(
                    "An unsigned array constraint cannot be applied to a {} type",
                    ty
                ),
            )),
        }
    }

    pub fn as_non_zero(&self, span: Span) -> Result<Constraint, Error> {
        match self {
            JavaType::Primitive(ty) if ty.int_like() => {
                Ok(Constraint::new(span, ConstraintKind::NonZero))
            }
            ty => Err(Error::new(
                span,
                format!("A non-zero constraint cannot be applied to a {} type", ty),
            )),
        }
    }

    pub fn as_natural(&self, span: Span) -> Result<Constraint, Error> {
        match self {
            JavaType::Primitive(ty) if ty.int_like() => {
                Ok(Constraint::new(span, ConstraintKind::Natural))
            }
            ty => Err(Error::new(
                span,
                format!(
                    "A natural number constraint cannot be applied to a {} type",
                    ty
                ),
            )),
        }
    }

    pub fn as_range(&self, span: Span, min: LitInt, max: LitInt) -> Result<Constraint, Error> {
        match self {
            JavaType::Primitive(ty) if ty.int_like() => {
                Ok(Constraint::new(span, ConstraintKind::InRange(min, max)))
            }
            ty => Err(Error::new(
                span,
                format!("A range constraint cannot be applied to a {} type", ty),
            )),
        }
    }

    pub fn unpack_default_value(&self, lit: Lit) -> Result<String, syn::Error> {
        let span = lit.span();
        let type_mismatch = |ty, span, lit| {
            Err(syn::Error::new(
                span,
                format!(
                    "Attempted to use a default value of '{:?}' on a field of type '{:?}'",
                    lit, ty
                ),
            ))
        };
        match (self, lit) {
            (JavaType::Primitive(ty), lit) => match (ty, lit) {
                (PrimitiveJavaType::Byte(_), Lit::Int(lit)) => {
                    Ok(lit.into_token_stream().to_string())
                }
                (PrimitiveJavaType::Int(_), Lit::Int(lit)) => {
                    Ok(lit.into_token_stream().to_string())
                }
                (PrimitiveJavaType::Long(_), Lit::Int(lit)) => {
                    Ok(lit.into_token_stream().to_string())
                }
                (PrimitiveJavaType::Float, Lit::Float(lit)) => {
                    Ok(lit.into_token_stream().to_string())
                }
                (PrimitiveJavaType::Double, Lit::Float(lit)) => {
                    Ok(lit.into_token_stream().to_string())
                }
                (PrimitiveJavaType::Boolean, Lit::Bool(lit)) => {
                    Ok(lit.into_token_stream().to_string())
                }
                (ty, lit) => type_mismatch(JavaType::Primitive(*ty), span, lit),
            },
            (JavaType::String, lit) => match lit {
                Lit::Str(lit) => Ok(lit.into_token_stream().to_string()),
                lit => type_mismatch(JavaType::String, span, lit),
            },
            (ty, def) => type_mismatch(*ty, span, def),
        }
    }
}

#[derive(Debug, thiserror::Error)]
#[error("Unsupported Rust type: {0}")]
pub struct UnsupportedType(String);

#[derive(Debug, Clone)]
pub struct JavaField {
    pub name: String,
    pub documentation: Documentation,
    pub ty: JavaType,
    pub default_value: String,
    pub constraint: Constraint,
}

impl JavaField {
    pub fn default_value(&self) -> String {
        self.default_value.clone()
    }
}

#[derive(Debug, Clone)]
pub struct Constraint {
    span: Span,
    kind: ConstraintKind,
}

#[derive(Debug, Clone)]
pub enum ConstraintKind {
    None,
    NonZero,
    Unsigned,
    UnsignedArray,
    Natural,
    InRange(LitInt, LitInt),
}

trait IntRange: FromStr + Display {
    fn min() -> Self;

    fn max() -> Self;
}

impl IntRange for u8 {
    fn min() -> Self {
        u8::MIN
    }

    fn max() -> Self {
        u8::MIN
    }
}

impl IntRange for u32 {
    fn min() -> Self {
        u32::MIN
    }

    fn max() -> Self {
        u32::MIN
    }
}

impl IntRange for u64 {
    fn min() -> Self {
        u64::MIN
    }

    fn max() -> Self {
        u64::MIN
    }
}

impl Constraint {
    pub fn new(span: Span, kind: ConstraintKind) -> Constraint {
        Constraint { span, kind }
    }

    fn validate_unsigned<T>(constraint: &mut Constraint) -> Result<(), Error>
    where
        T: IntRange,
        T::Err: Display,
    {
        let Constraint { span, kind } = constraint;
        match kind {
            ConstraintKind::None => {
                *kind = ConstraintKind::Unsigned;
                Ok(())
            }
            ConstraintKind::NonZero => Err(Error::new(
                span.clone(),
                "Cannot use a non-zero constraint on an unsigned type",
            )),
            ConstraintKind::Unsigned => Ok(()),
            ConstraintKind::UnsignedArray => {
                unreachable!()
            }
            ConstraintKind::Natural => Ok(()),
            ConstraintKind::InRange(min, max) => {
                let min_val = min.base10_parse::<T>();
                let max_val = max.base10_parse::<T>();

                let err = |span| {
                    Err(Error::new(
                        span,
                        format!(
                            "Unsigned number must be in range {}..={}",
                            T::min(),
                            T::max()
                        ),
                    ))
                };

                match (min_val, max_val) {
                    (Ok(_), Ok(_)) => Ok(()),
                    (Err(_), Ok(_)) => err(min.span()),
                    (Ok(_), Err(_)) => err(max.span()),
                    (Err(_), Err(_)) => err(min.span()),
                }
            }
        }
    }

    pub fn implicit(constraint: &mut Constraint, ty: &JavaType) -> Result<(), Error> {
        match ty {
            JavaType::Primitive(ty) => match ty {
                PrimitiveJavaType::Byte(true) => Self::validate_unsigned::<u8>(constraint),
                PrimitiveJavaType::Int(true) => Self::validate_unsigned::<u32>(constraint),
                PrimitiveJavaType::Long(true) => Self::validate_unsigned::<u64>(constraint),
                _ => Ok(()),
            },
            JavaType::Array(ty) => match ty {
                PrimitiveJavaType::Byte(true) => {
                    constraint.kind = ConstraintKind::UnsignedArray;
                    Ok(())
                }
                PrimitiveJavaType::Int(true) => {
                    constraint.kind = ConstraintKind::UnsignedArray;
                    Ok(())
                }
                PrimitiveJavaType::Long(true) => {
                    constraint.kind = ConstraintKind::UnsignedArray;
                    Ok(())
                }
                _ => Ok(()),
            },
            _ => Ok(()),
        }
    }

    pub fn apply_to_docs(&self, field_name: &str, documentation: &mut Documentation) {
        let Constraint { kind, .. } = self;
        match kind {
            ConstraintKind::None => {}
            ConstraintKind::NonZero => documentation.add_throws(
                "IllegalArgumentException",
                format!("if {} is zero", field_name),
            ),
            ConstraintKind::InRange(min, max) => documentation.add_throws(
                "IllegalArgumentException",
                format!(
                    "if '{}' is not in range {}..{}.",
                    field_name,
                    min,
                    max.to_string()
                ),
            ),
            ConstraintKind::Natural => documentation.add_throws(
                "IllegalArgumentException",
                format!("If '{}' is not a natural number (< 1).", field_name),
            ),
            ConstraintKind::UnsignedArray => documentation.add_throws(
                "IllegalArgumentException",
                format!("if {} contains negative elements", field_name),
            ),
            ConstraintKind::Unsigned => documentation.add_throws(
                "IllegalArgumentException",
                format!("if {} is negative", field_name),
            ),
        }
    }

    fn as_block(&self, field_name: &str) -> Block {
        let Constraint { kind, .. } = self;
        match kind {
            ConstraintKind::None => Block::default(),
            ConstraintKind::NonZero => Block::of(format!("if ({field_name} == 0) {{"))
                .add_statement(format!(
                    "{INDENTATION}throw new IllegalArgumentException(\"'{field_name}' must be non-zero\")"
                ))
                .add_line("}"),
            ConstraintKind::Unsigned => {
                Block::of(format!("if ({field_name} < 0) {{"))
                    .add_statement(format!(
                        "{INDENTATION}throw new IllegalArgumentException(\"'{field_name}' must be positive\")"
                    ))
                    .add_line("}")
            }
            ConstraintKind::UnsignedArray => {
                Block::of(format!("for (byte b : {field_name}) {{"))
                    .add_line(format!("{INDENTATION}if (b < 0) {{"))
                    .add_statement(format!("{INDENTATION}{INDENTATION}throw new IllegalArgumentException(\"'{field_name}' contains negative numbers\")"))
                    .add_line(format!("{INDENTATION}}}"))
                    .add_line("}")
            }
            ConstraintKind::InRange(min, max) => {
                Block::of(format!("if ({field_name} < {min} || {field_name} > {max}) {{"))
                    .add_statement(format!("{INDENTATION}throw new IllegalArgumentException(\"'{field_name}' must be in range {min}..{max}\")"))
                    .add_line("}")}
            ConstraintKind::Natural => {
                Block::of(format!("if ({field_name} < 1) {{"))
                    .add_statement(format!("{INDENTATION}throw new IllegalArgumentException(\"'{field_name}' must be a natural number\")"))
                    .add_line("}")
            }
        }
    }
}

#[derive(Debug)]
pub struct JavaMethodParameter {
    pub name: String,
    pub ty: JavaType,
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

#[derive(Default, Debug)]
pub struct Block {
    lines: Vec<String>,
}

impl Block {
    pub fn of(line: impl ToString) -> Block {
        let block = Block::default();
        block.add_line(line)
    }

    pub fn of_statement(line: impl ToString) -> Block {
        let block = Block::default();
        block.add_statement(line)
    }

    pub fn write(self, writer: &mut Writer) -> io::Result<()> {
        let block = self
            .lines
            .into_iter()
            .map(|line| {
                if line.starts_with('\n') || line.ends_with('\n') {
                    line
                } else {
                    format!("{line}\n")
                }
            })
            .collect::<String>();
        writer.write_all_indented(block.lines(), false)
    }

    pub fn add(mut self, line: impl ToString) -> Block {
        match self.lines.last_mut() {
            Some(last) => {
                *last = format!("{last} {}", line.to_string());
                self
            }
            None => self.add_line(line),
        }
    }

    pub fn add_line(mut self, line: impl ToString) -> Block {
        self.lines.push(line.to_string());
        self
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

#[derive(Debug)]
pub struct JavaMethod {
    pub name: String,
    pub documentation: Documentation,
    pub return_type: JavaType,
    pub args: Vec<JavaMethodParameter>,
    pub body: Block,
}

impl JavaMethod {
    pub fn new(name: impl ToString, return_type: JavaType) -> JavaMethod {
        JavaMethod {
            name: name.to_string(),
            documentation: Documentation::empty_documentation(),
            return_type,
            args: vec![],
            body: Block::default(),
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
        self.documentation.push_header_line(line.to_string());
        self
    }

    pub fn add_statement(mut self, statement: impl ToString) -> JavaMethod {
        self.body = self.body.add_statement(statement);
        self
    }

    pub fn getter_for(field: JavaField) -> JavaMethod {
        let documentation = Documentation::for_getter(field.name.clone(), field.default_value());
        let body = Block::of_statement(format!("return this.{}", field.name));

        JavaMethod {
            name: format!("get{}", AsUpperCamelCase(field.name)),
            documentation,
            return_type: field.ty,
            args: vec![],
            body,
        }
    }

    pub fn setter_for(field: JavaField) -> JavaMethod {
        let mut documentation = Documentation::for_setter(field.name.clone());
        field
            .constraint
            .apply_to_docs(&field.name, &mut documentation);

        let body = field
            .constraint
            .as_block(&field.name)
            .add_statement(format!("this.{} = {}", field.name, field.name));
        let arg = JavaMethodParameter::from_field(&field);

        JavaMethod {
            name: format!("set{}", AsUpperCamelCase(field.name)),
            documentation,
            return_type: JavaType::Void,
            args: vec![arg],
            body,
        }
    }
}
