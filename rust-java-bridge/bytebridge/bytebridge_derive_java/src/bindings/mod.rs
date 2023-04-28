mod java;
mod rust;

use crate::bindings::java::{
    validate_identifier, AbstractClassBuilder, ClassBuilder, JavaBindings,
};
use crate::bindings::rust::RustBindings;
use crate::docs::Documentation;
use crate::FormatStyle;
pub use java::{JavaSourceWriter, JavaSourceWriterBuilder};
use proc_macro2::Span;
pub use rust::{RustSourceWriter, RustSourceWriterBuilder};
use std::fmt::Display;
use std::path::PathBuf;
use std::{fs, io};
use syn::punctuated::Punctuated;
use syn::spanned::Spanned;
use syn::visit::Visit;
use syn::{
    Attribute, Error, Expr, ExprLit, Fields, Generics, Item, Lit, Meta, MetaNameValue, Token,
};

const MACRO_PATH: &str = "bytebridge";
const UNKNOWN_ATTRIBUTE: &str = "Unknown attribute";
const INVALID_PROPERTY_NAME: &str = "Invalid property name";

const ATTR_DOC: &str = "doc";
const ATTR_NO_DOCS: &str = "no_docs";
const ATTR_RENAME: &str = "rename";
const ATTR_DEFAULT_VALUE: &str = "default_value";
const ATTR_NON_ZERO: &str = "non_zero";
const ATTR_NATURAL: &str = "natural";
const ATTR_UNSIGNED_ARRAY: &str = "unsigned_array";
const ATTR_RANGE: &str = "range";

/// Arguments derived for either a struct or enum from its attributes.
pub struct DeriveArgs {
    /// The name of the item.
    pub name: String,
    /// Whether to infer documentation from Rust documentation attributes.
    pub infer_docs: bool,
    /// Any root-level documentation to apply to the class.
    pub root_doc: Documentation,
}

/// A builder for derivation arguments. If the internal state of the builder is Ok(None) *after* it
/// has been called, then it did not encounter a #[bytebridge] attribute and progress should be
/// halted in the derivation.
pub struct DeriveArgsBuilder {
    args: Result<Option<DeriveArgs>, Error>,
}

impl Default for DeriveArgsBuilder {
    fn default() -> Self {
        DeriveArgsBuilder { args: Ok(None) }
    }
}

impl DeriveArgsBuilder {
    /// Enter derivation for a class of name 'name'.
    fn enter(&mut self, name: String) {
        self.args = Ok(Some(DeriveArgs {
            name,
            infer_docs: true,
            root_doc: Documentation::from_style(FormatStyle::Documentation),
        }));
    }

    /// Set the state of the visit to an error.
    fn err(&mut self, span: Span, message: impl Display) {
        self.args = Err(Error::new(span, message))
    }

    /// Consume the state of the builder.
    fn into_result(self) -> Result<Option<DeriveArgs>, Error> {
        self.args
    }

    /// Set that documentation should be inferred.
    fn set_infer_docs(&mut self, to: bool) {
        if let Ok(Some(args)) = &mut self.args {
            args.infer_docs = to;
        }
    }

    /// Rename the item.
    fn set_name(&mut self, to: String) {
        if let Ok(Some(args)) = &mut self.args {
            args.name = to;
        }
    }

    /// Append a line of user-provided documentation.
    fn add_doc(&mut self, str: String) {
        if let Ok(Some(args)) = &mut self.args {
            args.root_doc.push_header_line(str);
        }
    }
}

impl<'ast> Visit<'ast> for DeriveArgsBuilder {
    fn visit_attribute(&mut self, i: &'ast Attribute) {
        if !i.path().is_ident(MACRO_PATH) {
            return;
        }

        if i.meta.require_list().is_err() {
            // Empty arguments in the attribute. This is valid and just means that the item has been
            // decorated as:
            //
            // #[bytebridge]
            // struct S {
            //      a: i32,
            //      b: i32
            // }
            return;
        }

        match i.parse_args_with(Punctuated::<Meta, Token![,]>::parse_terminated) {
            Ok(nested) => {
                for nested in nested {
                    match nested {
                        Meta::Path(path) if path.is_ident(ATTR_NO_DOCS) => {
                            self.set_infer_docs(false)
                        }
                        Meta::NameValue(MetaNameValue {
                            path,
                            value:
                                Expr::Lit(ExprLit {
                                    lit: Lit::Str(str), ..
                                }),
                            ..
                        }) if path.is_ident(ATTR_DOC) => self.add_doc(str.value()),
                        Meta::NameValue(meta) if meta.path.is_ident(ATTR_RENAME) => {
                            let span = meta.span();
                            match validate_identifier(meta) {
                                Ok(name) => self.set_name(name),
                                Err(_) => self.err(span, INVALID_PROPERTY_NAME),
                            }
                        }
                        meta => self.err(meta.span(), UNKNOWN_ATTRIBUTE),
                    }
                }
            }
            Err(e) => self.err(i.span(), e),
        }
    }

    fn visit_fields(&mut self, i: &'ast Fields) {
        let span = i.span();
        match i {
            Fields::Named(_) => {}
            Fields::Unnamed(_) => self.err(span, "Tuple structs are not supported"),
            Fields::Unit => self.err(span, "Struct must contain at least one field"),
        }
    }

    fn visit_generics(&mut self, i: &'ast Generics) {
        if !i.params.is_empty() {
            self.err(i.span(), "Generics are not supported")
        }
    }

    fn visit_item(&mut self, i: &Item) {
        match i {
            Item::Struct(item) => {
                self.enter(item.ident.to_string());

                self.visit_generics(&item.generics);
                self.visit_fields(&item.fields);

                for attr in &item.attrs {
                    self.visit_attribute(attr);
                }
            }
            Item::Enum(item) => {
                self.enter(item.ident.to_string());
                self.visit_generics(&item.generics);

                for attr in &item.attrs {
                    self.visit_attribute(attr);
                }

                for variant in &item.variants {
                    self.visit_fields(&variant.fields);
                }
            }
            _ => {}
        }
    }
}

/// Attempts to generate bindings for 'sources' and if they are valid them write them using the
/// provided writers.
pub fn generate_bindings(
    sources: Vec<PathBuf>,
    mut java_writer: JavaSourceWriter,
    mut rust_writer: RustSourceWriter,
) -> Result<(), crate::Error> {
    for path in sources {
        generate_binding(path, &mut java_writer, &mut rust_writer)?;
    }

    rust_writer.complete()?;

    Ok(())
}

/// Generate bindings from the provided file at 'path' and if they are valid them write them using
/// the provided writers.
fn generate_binding(
    path: PathBuf,
    java_writer: &mut JavaSourceWriter,
    rust_writer: &mut RustSourceWriter,
) -> Result<(), crate::Error> {
    let file = syn::parse_file(&fs::read_to_string(path)?)?;
    for item in file.items {
        if let Some(bindings) = derive_bindings(item)? {
            bindings.write(java_writer, rust_writer)?;
        }
    }
    Ok(())
}

/// Returns whether derivation should be attempted on an item. This is only valid iff the item is an
/// enumeration or a struct.
fn should_enter(item: &Item) -> bool {
    let predicate = |attrs: &[Attribute]| attrs.iter().any(|attr| attr.path().is_ident(MACRO_PATH));
    match item {
        Item::Enum(item) => predicate(&item.attrs),
        Item::Struct(item) => predicate(&item.attrs),
        _ => false,
    }
}

fn derive_bindings(mut item: Item) -> Result<Option<Bindings>, Error> {
    if !should_enter(&item) {
        return Ok(None);
    }

    let mut args_builder = DeriveArgsBuilder::default();
    args_builder.visit_item(&mut item);

    let args = match args_builder.into_result()? {
        Some(args) => args,
        None => return Ok(None),
    };

    let java = match &mut item {
        Item::Enum(item) => {
            let mut java_binding_builder =
                AbstractClassBuilder::new(args.infer_docs, args.root_doc, args.name);
            java_binding_builder.visit_item_enum(item);
            JavaBindings::Abstract(java_binding_builder.into_result()?)
        }
        Item::Struct(item) => {
            let mut java_binding_builder =
                ClassBuilder::new(args.infer_docs, args.root_doc, args.name);
            java_binding_builder.visit_item_struct(item);
            JavaBindings::Class(java_binding_builder.into_result()?)
        }
        i => {
            // Unreachable as no args would have been returned above
            unreachable!("{:?}", i);
        }
    };

    Ok(Some(Bindings {
        java,
        rust: RustBindings::build(item),
    }))
}

/// Derived bindings for both Java and Rust.
struct Bindings {
    java: JavaBindings,
    rust: RustBindings,
}

impl Bindings {
    fn write(
        self,
        java_writer: &mut JavaSourceWriter,
        rust_writer: &mut RustSourceWriter,
    ) -> io::Result<()> {
        let Bindings { java, rust } = self;

        java.write(java_writer)?;
        rust.write(rust_writer)
    }
}
