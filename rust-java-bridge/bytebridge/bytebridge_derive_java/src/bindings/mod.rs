mod java;
mod rust;

use crate::bindings::java::{AbstractClassBuilder, ClassBuilder, JavaBindings};
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

pub struct DeriveArgs {
    pub infer_docs: bool,
    pub root_doc: Documentation,
}

impl Default for DeriveArgs {
    fn default() -> Self {
        DeriveArgs {
            infer_docs: true,
            root_doc: Documentation::from_style(FormatStyle::Documentation),
        }
    }
}

pub struct DeriveArgsBuilder {
    result: Result<DeriveArgs, Error>,
}

impl Default for DeriveArgsBuilder {
    fn default() -> Self {
        DeriveArgsBuilder {
            result: Ok(DeriveArgs::default()),
        }
    }
}

impl DeriveArgsBuilder {
    fn err(&mut self, span: Span, message: impl Display) {
        self.result = Err(Error::new(span, message))
    }

    fn into_result(self) -> Result<DeriveArgs, Error> {
        self.result
    }

    fn set_infer_docs(&mut self, to: bool) {
        match &mut self.result {
            Ok(ctx) => {
                ctx.infer_docs = to;
            }
            Err(_) => {}
        }
    }

    fn add_doc(&mut self, str: String) {
        match &mut self.result {
            Ok(ctx) => {
                ctx.root_doc.push_header_line(str);
            }
            Err(_) => {}
        }
    }
}

impl<'ast> Visit<'ast> for DeriveArgsBuilder {
    fn visit_attribute(&mut self, i: &'ast Attribute) {
        const UNKNOWN_ATTRIBUTE: &str = "Unknown attribute";

        if !i.path().is_ident(MACRO_PATH) {
            return;
        }

        if let Meta::Path(_) = &i.meta {
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
                        Meta::Path(path) if path.is_ident("no_docs") => self.set_infer_docs(false),
                        Meta::NameValue(MetaNameValue {
                            path,
                            value:
                                Expr::Lit(ExprLit {
                                    lit: Lit::Str(str), ..
                                }),
                            ..
                        }) if path.is_ident("doc") => self.add_doc(str.value()),
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
                self.visit_generics(&item.generics);
                self.visit_fields(&item.fields);

                for attr in &item.attrs {
                    self.visit_attribute(attr);
                }
            }
            Item::Enum(item) => {
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

fn derive_bindings(mut item: Item) -> Result<Option<Bindings>, Error> {
    if !should_enter(&item) {
        return Ok(None);
    }

    let mut args_builder = DeriveArgsBuilder::default();
    args_builder.visit_item(&mut item);

    let args = args_builder.into_result()?;

    let java = match &mut item {
        Item::Enum(item) => {
            let mut java_binding_builder =
                AbstractClassBuilder::new(args.infer_docs, args.root_doc, item.ident.to_string());
            java_binding_builder.visit_item_enum(item);
            JavaBindings::Abstract(java_binding_builder.into_result()?)
        }
        Item::Struct(item) => {
            let mut java_binding_builder =
                ClassBuilder::new(args.infer_docs, args.root_doc, item.ident.to_string());
            java_binding_builder.visit_item_struct(item);
            JavaBindings::Class(java_binding_builder.into_result()?)
        }
        i => {
            // Unreachable due to should_enter call above
            unreachable!("{:?}", i);
        }
    };

    Ok(Some(Bindings {
        java,
        rust: RustBindings::build(item),
    }))
}

fn should_enter(item: &Item) -> bool {
    let predicate = |attrs: &[Attribute]| attrs.iter().any(|attr| attr.path().is_ident(MACRO_PATH));

    match item {
        Item::Enum(item) => predicate(&item.attrs),
        Item::Struct(item) => predicate(&item.attrs),
        _ => false,
    }
}

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
