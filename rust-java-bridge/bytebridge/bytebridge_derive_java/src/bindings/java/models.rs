// Copyright 2015-2024 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
use syn::spanned::Spanned;
use syn::{Error, Lit, LitInt, PathArguments};

pub const BUFFER_SIZE_VAR: &str = "__buf__size";
pub const PACKER_VAR: &str = "__packer";
pub const TEMP_VAR: &str = "__elem_";
pub const PACK_METHOD: &str = "pack";
pub const TO_STRING_METHOD: &str = "toString";

pub const RESERVED_VARIABLES: [&str; 3] = [BUFFER_SIZE_VAR, PACK_METHOD, TEMP_VAR];

lazy_static! {
    pub static ref JAVA_KEYWORDS: HashSet<&'static str> = {
        HashSet::from([
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
        ])
    };
}

#[derive(Copy, Clone)]
pub enum RustType {
    I8,
    I16,
    I32,
    I64,
    U8,
    U16,
    U32,
    U64,
    F32,
    F64,
    Duration,
    NonZeroU32,
    NonZeroU64,
    String,
    Bool,
    Char,
    Vec,
}

/// Discriminate between primitive Java types and arrays.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum JavaType {
    Void,
    String,
    Primitive(PrimitiveJavaType),
    Array(PrimitiveJavaType),
    Object(String),
}

impl JavaType {
    pub fn component_type(&self) -> JavaType {
        match self {
            JavaType::Void => JavaType::Void,
            JavaType::String => JavaType::String,
            JavaType::Primitive(ty) => JavaType::Primitive(*ty),
            JavaType::Array(ty) => JavaType::Primitive(*ty),
            JavaType::Object(ty) => JavaType::Object(ty.clone()),
        }
    }
}

/// Attempts to map a Rust type to a Java Type.
pub fn try_map_type(
    ident: &str,
    arguments: &PathArguments,
) -> Result<(JavaType, RustType), UnsupportedType> {
    match ident {
        "i8" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Byte {
                unsigned: false,
                nonzero: false,
            }),
            RustType::I8,
        )),
        "i16" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Int {
                unsigned: false,
                nonzero: false,
            }),
            RustType::I16,
        )),
        "i32" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Int {
                unsigned: false,
                nonzero: false,
            }),
            RustType::I32,
        )),
        "i64" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Long {
                unsigned: false,
                nonzero: false,
            }),
            RustType::I64,
        )),
        "u8" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Byte {
                unsigned: true,
                nonzero: false,
            }),
            RustType::U8,
        )),
        "u16" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Short {
                unsigned: true,
                nonzero: false,
            }),
            RustType::U16,
        )),
        "u32" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Int {
                unsigned: true,
                nonzero: false,
            }),
            RustType::U32,
        )),
        "u64" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Long {
                unsigned: true,
                nonzero: false,
            }),
            RustType::U64,
        )),
        "f32" => Ok((JavaType::Primitive(PrimitiveJavaType::Float), RustType::F32)),
        "f64" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Double),
            RustType::F64,
        )),
        "Duration" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Int {
                unsigned: true,
                nonzero: false,
            }),
            RustType::Duration,
        )),
        "NonZeroU32" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Int {
                unsigned: true,
                nonzero: true,
            }),
            RustType::NonZeroU32,
        )),
        "NonZeroU64" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Long {
                unsigned: true,
                nonzero: true,
            }),
            RustType::NonZeroU64,
        )),
        "String" => Ok((JavaType::String, RustType::String)),
        "bool" => Ok((
            JavaType::Primitive(PrimitiveJavaType::Boolean),
            RustType::Bool,
        )),
        "char" => Ok((JavaType::Primitive(PrimitiveJavaType::Char), RustType::Char)),
        "Vec" => {
            let err = || {
                Err(UnsupportedType(format!(
                    "Vec{}",
                    arguments.to_token_stream()
                )))
            };
            match arguments {
                PathArguments::AngleBracketed(ang) => {
                    let mut iter = ang.args.pairs();
                    match (iter.next(), iter.next()) {
                        (Some(pair), None) => {
                            let value = pair.value();
                            match try_map_type(
                                value.to_token_stream().to_string().as_str(),
                                &PathArguments::None,
                            ) {
                                Ok((JavaType::Primitive(ty), _)) => {
                                    Ok((JavaType::Array(ty), RustType::Vec))
                                }
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
            JavaType::Object(ty) => {
                write!(f, "{ty}")
            }
        }
    }
}

/// A primitive unboxed Java type.
#[derive(Debug, Copy, Clone, PartialEq, Eq)]
pub enum PrimitiveJavaType {
    Byte { unsigned: bool, nonzero: bool },
    Int { unsigned: bool, nonzero: bool },
    Long { unsigned: bool, nonzero: bool },
    Short { unsigned: bool, nonzero: bool },
    Float,
    Double,
    Boolean,
    Char,
}

impl PrimitiveJavaType {
    pub fn int_like(&self) -> bool {
        matches!(
            self,
            PrimitiveJavaType::Int { .. }
                | PrimitiveJavaType::Long { .. }
                | PrimitiveJavaType::Byte { .. }
                | PrimitiveJavaType::Short { .. }
        )
    }

    pub fn size_of(&self) -> usize {
        match self {
            PrimitiveJavaType::Byte { .. } => 1,
            PrimitiveJavaType::Short { .. } => 2,
            PrimitiveJavaType::Int { .. } => 4,
            PrimitiveJavaType::Long { .. } => 8,
            PrimitiveJavaType::Float => 4,
            PrimitiveJavaType::Double => 8,
            PrimitiveJavaType::Boolean => 1,
            PrimitiveJavaType::Char => 1,
        }
    }
}

impl Display for PrimitiveJavaType {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            PrimitiveJavaType::Byte { .. } => {
                write!(f, "byte")
            }
            PrimitiveJavaType::Short { .. } => {
                write!(f, "short")
            }
            PrimitiveJavaType::Int { .. } => {
                write!(f, "int")
            }
            PrimitiveJavaType::Long { .. } => {
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
            PrimitiveJavaType::Char => {
                write!(f, "char")
            }
        }
    }
}

impl JavaType {
    /// Returns the default value for this Java type.
    pub fn default_value(&self) -> String {
        match self {
            JavaType::Primitive(PrimitiveJavaType::Byte { .. }) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Int { .. }) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Long { .. }) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Float) => "0.0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Double) => "0.0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Boolean) => "false".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Short { .. }) => "0".to_string(),
            JavaType::Primitive(PrimitiveJavaType::Char) => "u0000".to_string(),
            JavaType::Void => "".to_string(),
            JavaType::String => "\"\"".to_string(),
            JavaType::Array(arr) => {
                format!("new {}[] {{}}", arr)
            }
            JavaType::Object(_) => "null".to_string(),
        }
    }

    /// Attempts to build an unsigned array constraint from this Java type. This will fail if the
    /// type is not an array.
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

    /// Attempts to build an non-zero constraint from this Java type. This will fail if the type is
    /// not primitive number type.
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

    /// Attempts to build a natural number constraint from this Java type. This will fail if the
    /// type is not primitive number type.
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

    /// Attempts to build a ranged number constraint from this Java type. This will fail if the type
    /// is not primitive number type.
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

    /// Attempts to unpack a default value that has been applied in an attribute against this Java
    /// type. This will fail, for example, if this Java Type is a string and a numeric literal has
    /// been supplied.
    pub fn unpack_default_value(&self, lit: Lit, rust_type: RustType) -> Result<String, Error> {
        let span = lit.span();
        let type_mismatch = |ty, span, lit| {
            Err(Error::new(
                span,
                format!(
                    "Attempted to use a default value of '{:?}' on a field of type '{:?}'",
                    lit, ty
                ),
            ))
        };
        match (self, lit) {
            (JavaType::Primitive(ty), lit) => {
                match (ty, lit, rust_type) {
                    (PrimitiveJavaType::Byte { .. }, Lit::Int(lit), _) => {
                        Ok(lit.into_token_stream().to_string())
                    }
                    (PrimitiveJavaType::Int { .. }, Lit::Int(lit), _) => {
                        Ok(lit.into_token_stream().to_string())
                    }
                    (PrimitiveJavaType::Int { .. }, Lit::Str(str), RustType::Duration) => {
                        let mut value = str.value();
                        match value.pop() {
                            Some(last) => {
                                if last == 's' {
                                    Ok(value)
                                } else {
                                    Err(Error::new(str.span(), format!("Invalid duration time format: {}. Only 's' is accepted", last)))
                                }
                            }
                            None => Err(Error::new(str.span(), "Empty default value")),
                        }
                    }
                    (PrimitiveJavaType::Long { .. }, Lit::Int(lit), _) => {
                        Ok(lit.into_token_stream().to_string())
                    }
                    (PrimitiveJavaType::Float, Lit::Float(lit), _) => {
                        Ok(lit.into_token_stream().to_string())
                    }
                    (PrimitiveJavaType::Double, Lit::Float(lit), _) => {
                        Ok(lit.into_token_stream().to_string())
                    }
                    (PrimitiveJavaType::Boolean, Lit::Bool(lit), _) => {
                        Ok(lit.into_token_stream().to_string())
                    }
                    (ty, lit, _) => type_mismatch(JavaType::Primitive(*ty), span, lit),
                }
            }
            (JavaType::String, lit) => match lit {
                Lit::Str(lit) => Ok(lit.value()),
                lit => type_mismatch(JavaType::String, span, lit),
            },
            (ty, def) => type_mismatch(ty.clone(), span, def),
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

/// A constraint that is applied to a field (this may represent none).
#[derive(Debug, Clone)]
pub struct Constraint {
    /// The source field's span.
    span: Span,
    /// The type of constraint.
    kind: ConstraintKind,
}

/// A possible numeric constraint to apply to a field when it is being updated through a setter.
#[derive(Debug, Clone)]
pub enum ConstraintKind {
    /// Nothing will be applied.
    None,
    /// The supplied value must be non-zero.
    NonZero,
    /// The supplied value must be positive.
    Unsigned,
    /// The supplied value must be an array containing positive values.
    UnsignedArray,
    /// The supplied value must be an array containing non-zero values.
    NonZeroArray,
    /// The supplied value must be a natural number.
    Natural,
    /// The supplied value must be in the provided range.
    InRange(LitInt, LitInt),
}

/// Helper trait for abstracting over non-zero types.
trait IntRange: FromStr + Display + Copy {
    /// Returns whether the supplied range contains zero.
    fn contains_zero(min: Self, max: Self) -> bool;

    fn one() -> Self;

    fn min() -> Self;

    fn max() -> Self;
}

impl IntRange for u8 {
    fn contains_zero(min: Self, max: Self) -> bool {
        (min..max).contains(&0)
    }

    fn one() -> Self {
        1
    }

    fn min() -> Self {
        u8::MIN
    }

    fn max() -> Self {
        u8::MAX
    }
}

impl IntRange for u16 {
    fn contains_zero(min: Self, max: Self) -> bool {
        (min..max).contains(&0)
    }

    fn one() -> Self {
        1
    }

    fn min() -> Self {
        u16::MIN
    }

    fn max() -> Self {
        u16::MAX
    }
}

impl IntRange for u32 {
    fn contains_zero(min: Self, max: Self) -> bool {
        (min..max).contains(&0)
    }

    fn one() -> Self {
        1
    }

    fn min() -> Self {
        u32::MIN
    }

    fn max() -> Self {
        u32::MAX
    }
}

impl IntRange for u64 {
    fn contains_zero(min: Self, max: Self) -> bool {
        (min..max).contains(&0)
    }

    fn one() -> Self {
        1
    }

    fn min() -> Self {
        u64::MIN
    }

    fn max() -> Self {
        u64::MAX
    }
}

impl Constraint {
    pub fn new(span: Span, kind: ConstraintKind) -> Constraint {
        Constraint { span, kind }
    }

    /// Attempts to step this constraint from None to a new constraint.
    pub fn step(&mut self, to: Constraint) -> Result<(), Error> {
        let Constraint { kind, .. } = self;
        match kind {
            ConstraintKind::None => {
                *self = to;
                Ok(())
            }
            _ => Err(Error::new(to.span, "Constraint has already been applied")),
        }
    }

    /// Returns whether the provided constraint is valid against a field type that is unsigned and
    /// possibly requires a non-zero value.
    ///
    /// It is possible for a field to be invalid if it is a non-zero type *and* a user has applied a
    /// ranged constraint that contains a lower bound that is lower than zero.
    fn validate_unsigned<T>(constraint: &mut Constraint, nonzero: bool) -> Result<(), Error>
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
                *span,
                "Cannot use a non-zero constraint on an unsigned type",
            )),
            ConstraintKind::Unsigned => Ok(()),
            ConstraintKind::UnsignedArray | ConstraintKind::NonZeroArray => {
                unreachable!()
            }
            ConstraintKind::Natural => Ok(()),
            ConstraintKind::InRange(min, max) => {
                let min_span = min.span();
                let max_span = max.span();
                let min_val = min.base10_parse::<T>();
                let max_val = max.base10_parse::<T>();

                let err = |span| {
                    let min = if nonzero { T::one() } else { T::min() };
                    Err(Error::new(
                        span,
                        format!("Number must be in range {}..={}", min, T::max()),
                    ))
                };

                match (min_val, max_val) {
                    (Ok(min), Ok(max)) => {
                        if T::contains_zero(min, max) && nonzero {
                            err(min_span)
                        } else {
                            Ok(())
                        }
                    }
                    (Err(_), Ok(_)) => err(min_span),
                    (Ok(_), Err(_)) => err(max_span),
                    (Err(_), Err(_)) => err(min_span),
                }
            }
        }
    }

    /// Infers an implicit constraint from a field type if it is an unsigned type.
    pub fn implicit(constraint: &mut Constraint, ty: &JavaType) -> Result<(), Error> {
        match ty {
            JavaType::Primitive(ty) => match ty {
                PrimitiveJavaType::Byte {
                    unsigned: true,
                    nonzero,
                } => Self::validate_unsigned::<u8>(constraint, *nonzero),
                PrimitiveJavaType::Short {
                    unsigned: true,
                    nonzero,
                } => Self::validate_unsigned::<u16>(constraint, *nonzero),
                PrimitiveJavaType::Int {
                    unsigned: true,
                    nonzero,
                } => Self::validate_unsigned::<u32>(constraint, *nonzero),
                PrimitiveJavaType::Long {
                    unsigned: true,
                    nonzero,
                } => Self::validate_unsigned::<u64>(constraint, *nonzero),
                _ => Ok(()),
            },
            JavaType::Array(ty) => match ty {
                PrimitiveJavaType::Byte {
                    unsigned: true,
                    nonzero,
                } => {
                    constraint.kind = if *nonzero {
                        ConstraintKind::NonZeroArray
                    } else {
                        ConstraintKind::UnsignedArray
                    };
                    Ok(())
                }
                PrimitiveJavaType::Int {
                    unsigned: true,
                    nonzero,
                } => {
                    constraint.kind = if *nonzero {
                        ConstraintKind::NonZeroArray
                    } else {
                        ConstraintKind::UnsignedArray
                    };
                    Ok(())
                }
                PrimitiveJavaType::Long {
                    unsigned: true,
                    nonzero,
                } => {
                    constraint.kind = if *nonzero {
                        ConstraintKind::NonZeroArray
                    } else {
                        ConstraintKind::UnsignedArray
                    };
                    Ok(())
                }
                _ => Ok(()),
            },
            _ => Ok(()),
        }
    }

    /// Applies this constraint's documentation into 'documentation'.
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
                format!("if '{}' is not in range {}..{}.", field_name, min, max),
            ),
            ConstraintKind::Natural => documentation.add_throws(
                "IllegalArgumentException",
                format!("If '{}' is not a natural number (< 1).", field_name),
            ),
            ConstraintKind::UnsignedArray => documentation.add_throws(
                "IllegalArgumentException",
                format!("if {} contains negative elements", field_name),
            ),
            ConstraintKind::NonZeroArray => documentation.add_throws(
                "IllegalArgumentException",
                format!("if {} contains elements equal to zero", field_name),
            ),
            ConstraintKind::Unsigned => documentation.add_throws(
                "IllegalArgumentException",
                format!("if {} is negative", field_name),
            ),
        }
    }

    /// Builds this constraint into a source code block.
    fn as_block(&self, field_name: &str, ty: JavaType) -> Block {
        let Constraint { kind, .. } = self;
        let component_type = ty.component_type();
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
                Block::of(format!("for ({component_type} b : {field_name}) {{"))
                    .add_line(format!("{INDENTATION}if (b < 0) {{"))
                    .add_statement(format!("{INDENTATION}{INDENTATION}throw new IllegalArgumentException(\"'{field_name}' contains negative numbers\")"))
                    .add_line(format!("{INDENTATION}}}"))
                    .add_line("}")
            }
            ConstraintKind::NonZeroArray => {
                Block::of(format!("for ({component_type} b : {field_name}) {{"))
                    .add_line(format!("{INDENTATION}if (b == 0) {{"))
                    .add_statement(format!("{INDENTATION}{INDENTATION}throw new IllegalArgumentException(\"'{field_name}' contains an element equal to zero\")"))
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

    fn from_field(name: &str, ty: JavaType) -> JavaMethodParameter {
        JavaMethodParameter {
            name: name.to_string(),
            ty,
        }
    }
}

/// A Java source code block builder.
#[derive(Default, Debug)]
pub struct Block {
    lines: Vec<String>,
}

impl Display for Block {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        let Block { lines } = self;
        for l in lines {
            write!(f, "{}", l)?;
        }

        Ok(())
    }
}

impl Block {
    /// Returns whether this source code block is empty.
    pub fn is_empty(&self) -> bool {
        self.lines.iter().all(|line| line.is_empty())
    }

    /// Builds a source code from a line.
    pub fn of(line: impl ToString) -> Block {
        let block = Block::default();
        block.add_line(line)
    }

    /// Builds a source code from a statement.
    pub fn of_statement(line: impl ToString) -> Block {
        let block = Block::default();
        block.add_statement(line)
    }

    /// Writes this source code block using 'writer'.
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

    /// Appends 'line' into the last line in this source code block.
    pub fn add(mut self, line: impl ToString) -> Block {
        match self.lines.last_mut() {
            Some(last) => {
                *last = format!("{last} {}", line.to_string());
                self
            }
            None => self.add_line(line),
        }
    }

    /// Appends 'line' into this source code block.
    pub fn add_line(mut self, line: impl ToString) -> Block {
        self.lines.push(line.to_string());
        self
    }

    /// Appends 'line' as a statement into this source code block.
    pub fn add_statement(mut self, statement: impl ToString) -> Block {
        self.lines.push(format!("{};", statement.to_string()));
        self
    }

    /// Extends this source code block with the content in 'with'.
    pub fn extend(mut self, with: Block) -> Block {
        self.lines.extend(with.lines);
        self
    }
}

#[derive(Debug)]
pub struct JavaMethod {
    pub name: String,
    pub documentation: Documentation,
    pub return_type: JavaType,
    pub args: Vec<JavaMethodParameter>,
    pub r#abstract: bool,
    pub body: Block,
    pub annotation: Option<String>,
    pub throws: Vec<String>,
}

impl JavaMethod {
    /// Builds a new Java Method definition.
    ///
    /// # Parameters
    /// - name: the name of the method
    /// - return_type: the return type of the method
    /// - annotation: an optional annotation that the method will be decorated by.
    pub fn new(
        name: impl ToString,
        return_type: JavaType,
        annotation: Option<String>,
    ) -> JavaMethod {
        JavaMethod {
            name: name.to_string(),
            documentation: Documentation::empty_documentation(),
            return_type,
            args: vec![],
            r#abstract: false,
            body: Block::default(),
            annotation,
            throws: vec![],
        }
    }

    /// Mark this method as abstract.
    pub fn set_abstract(mut self) -> JavaMethod {
        self.r#abstract = true;
        self
    }

    /// Sets this methods body.
    pub fn set_block(mut self, to: Block) -> JavaMethod {
        self.body = to;
        self
    }

    /// Adds a line of documentation to this method.
    pub fn add_documentation(mut self, line: impl ToString) -> JavaMethod {
        self.documentation.push_header_line(line.to_string());
        self
    }

    pub fn add_arg(mut self, name: impl ToString, ty: JavaType) -> JavaMethod {
        self.args.push(JavaMethodParameter {
            name: name.to_string(),
            ty,
        });
        self
    }

    /// Infers a Java Method getter from the provided field.
    pub fn getter_for(field: JavaField) -> JavaMethod {
        let documentation = Documentation::for_getter(field.name.clone(), field.default_value());
        let body = Block::of_statement(format!("return this.{}", field.name));

        JavaMethod {
            name: format!("get{}", AsUpperCamelCase(field.name)),
            documentation,
            return_type: field.ty,
            args: vec![],
            r#abstract: false,
            body,
            annotation: None,
            throws: vec![],
        }
    }

    pub fn add_throws(mut self, name: impl ToString) -> JavaMethod {
        self.throws.push(name.to_string());
        self
    }

    /// Infers a Java Method setter from the provided field.
    pub fn setter_for(field: JavaField) -> JavaMethod {
        let JavaField {
            name,
            ty,
            constraint,
            ..
        } = field;

        let mut documentation = Documentation::for_setter(name.clone());
        constraint.apply_to_docs(&name, &mut documentation);

        let body = constraint
            .as_block(&name, ty.clone())
            .add_statement(format!("this.{name} = {name}"));
        let arg = JavaMethodParameter::from_field(name.as_str(), ty);

        JavaMethod {
            name: format!("set{}", AsUpperCamelCase(name)),
            documentation,
            return_type: JavaType::Void,
            args: vec![arg],
            r#abstract: false,
            body,
            annotation: None,
            throws: vec![],
        }
    }
}
