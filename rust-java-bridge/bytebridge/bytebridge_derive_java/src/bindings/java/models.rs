use crate::bindings::java::writer::Writer;
use crate::docs::Documentation;
use heck::AsUpperCamelCase;
use lazy_static::lazy_static;
use quote::ToTokens;
use std::collections::HashSet;
use std::fmt::{Display, Formatter};
use std::io;
use syn::Lit;

pub const BUFFER_SIZE_VAR: &str = "__buf__size";
pub const BUFFER_VAR: &str = "__buf";
pub const AS_BYTES_METHOD: &str = "asBytes";

lazy_static! {
    pub static ref JAVA_KEYWORDS: HashSet<&'static str> = {
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

/// Discriminate between primitive Java types and arrays.
#[derive(Debug, Copy, Clone, PartialEq)]
pub enum JavaType {
    Void,
    String,
    Primitive(PrimitiveJavaType),
    Array(PrimitiveJavaType),
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
    Byte,
    Int,
    Long,
    Float,
    Double,
    Boolean,
}

impl PrimitiveJavaType {
    pub fn size_of(&self) -> usize {
        match self {
            PrimitiveJavaType::Byte => 1,
            PrimitiveJavaType::Int => 4,
            PrimitiveJavaType::Long => 8,
            PrimitiveJavaType::Float => 4,
            PrimitiveJavaType::Double => 8,
            PrimitiveJavaType::Boolean => 1,
        }
    }
}

impl Display for PrimitiveJavaType {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            PrimitiveJavaType::Byte => {
                write!(f, "byte")
            }
            PrimitiveJavaType::Int => {
                write!(f, "int")
            }
            PrimitiveJavaType::Long => {
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
            JavaType::Primitive(PrimitiveJavaType::Byte) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Int) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Long) => "0".to_string(),
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
                (PrimitiveJavaType::Byte, Lit::Int(lit)) => Ok(lit.into_token_stream().to_string()),
                (PrimitiveJavaType::Int, Lit::Int(lit)) => Ok(lit.into_token_stream().to_string()),
                (PrimitiveJavaType::Long, Lit::Int(lit)) => Ok(lit.into_token_stream().to_string()),
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

impl TryFrom<&str> for JavaType {
    type Error = UnsupportedType;

    fn try_from(value: &str) -> Result<Self, Self::Error> {
        match value {
            "i8" => Ok(JavaType::Primitive(PrimitiveJavaType::Byte)),
            "i32" => Ok(JavaType::Primitive(PrimitiveJavaType::Int)),
            "i64" => Ok(JavaType::Primitive(PrimitiveJavaType::Long)),
            "f32" => Ok(JavaType::Primitive(PrimitiveJavaType::Float)),
            "f64" => Ok(JavaType::Primitive(PrimitiveJavaType::Double)),
            "Duration" => Ok(JavaType::Primitive(PrimitiveJavaType::Int)),
            "String" => Ok(JavaType::String),
            "bool" => Ok(JavaType::Primitive(PrimitiveJavaType::Boolean)),
            v => Err(UnsupportedType(v.to_string())),
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
}

impl JavaField {
    pub fn default_value(&self) -> String {
        self.default_value.clone()
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

#[derive(Debug)]
pub struct FieldModifiers {
    pub r#static: bool,
    pub protected: bool,
    pub public: bool,
    pub r#final: bool,
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

    pub fn write(self, writer: &mut Writer) -> io::Result<()> {
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

#[derive(Default, Debug)]
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

    pub fn add_line(mut self, line: impl ToString) -> Block {
        self.lines.push(line.to_string());
        self
    }

    pub fn add_statement(mut self, statement: impl ToString) -> Block {
        self.lines.push(format!("{};\n", statement.to_string()));
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
