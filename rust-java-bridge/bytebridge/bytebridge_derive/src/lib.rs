#![allow(warnings)]

use std::default::Default;
use std::fmt::Write;
use std::fmt::{Display, Formatter};
use std::fs::File;
use std::io::{BufReader, BufWriter, Read, Stdout};
use std::path::{Path, PathBuf};
use std::rc::Rc;
use std::{fs, io};

use quote::ToTokens;
use syn::Lit;

pub use bindings::JavaSourceWriterBuilder;
pub use docs::FormatStyle;
use error::Error;

use crate::bindings::generate_bindings;

mod bindings;
mod docs;
mod error;

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
    fn default_value(&self) -> String {
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

    fn unpack_default_value(&self, lit: Lit) -> Result<String, syn::Error> {
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

#[derive(Debug, Default)]
pub struct Builder {
    input_files: Vec<PathBuf>,
}

impl Builder {
    pub fn add_source(mut self, path: PathBuf) -> Builder {
        self.input_files.push(path);
        self
    }

    pub fn generate(self, java_writer: JavaSourceWriterBuilder) -> Result<(), Error> {
        let Builder { input_files } = self;
        generate_bindings(input_files, java_writer.build())
    }
}
