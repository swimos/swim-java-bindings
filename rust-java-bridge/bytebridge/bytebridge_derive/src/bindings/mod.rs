mod java;
mod rust;

use crate::bindings::java::{JavaBinding, JavaSourceWriter};
use crate::docs::{Documentation, FormatStyle};
pub use java::JavaSourceWriterBuilder;
use proc_macro2::Ident;
use quote::ToTokens;
use std::fs;
use std::io::{BufWriter, Write};
use std::net::TcpStream;
use std::path::PathBuf;
use syn::parse::{Parse, ParseBuffer, ParseStream};
use syn::spanned::Spanned;
use syn::{
    Attribute, Data, DataEnum, DataStruct, DeriveInput, Fields, Generics, Item, ItemEnum,
    ItemMacro, ItemStruct, ItemType, Lit, Meta, MetaList, NestedMeta, Type, Visibility,
};

const MACRO_PATH: &str = "bytebridge";

pub fn generate_bindings(
    sources: Vec<PathBuf>,
    mut java_writer: JavaSourceWriter,
) -> Result<(), crate::Error> {
    for path in sources {
        generate_java_binding(path, &mut java_writer)?;
    }
    Ok(())
}

fn try_strip_args(attrs: &mut Vec<Attribute>) -> Result<Option<DeriveArgs>, syn::Error> {
    let mut derive_args = DeriveArgs::default();
    let mut attribute_iter = attrs
        .iter()
        .filter(|at| at.path.is_ident(MACRO_PATH))
        .peekable();

    if attribute_iter.peek().is_none() {
        return Ok(None);
    }

    let unknown_attribute = |span| Err(syn::Error::new(span, "Unknown attribute"));

    for attr in attribute_iter {
        match attr.parse_meta()? {
            Meta::List(args) => {
                for nested in args.nested {
                    match nested {
                        NestedMeta::Meta(Meta::Path(path)) if path.is_ident("no_docs") => {
                            derive_args.infer_docs = false;
                        }
                        NestedMeta::Meta(Meta::NameValue(meta)) if meta.path.is_ident("doc") => {
                            match meta.lit {
                                Lit::Str(str) => derive_args.add_doc(str.value()),
                                list => return unknown_attribute(list.span()),
                            }
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

    attrs.retain(|at| !at.path.is_ident(MACRO_PATH));
    Ok(Some(derive_args))
}

fn generate_java_binding(path: PathBuf, writer: &mut JavaSourceWriter) -> Result<(), crate::Error> {
    let file = syn::parse_file(&fs::read_to_string(path)?)?;
    for item in file.items {
        let bindings = match item {
            Item::Struct(mut item) => match try_strip_args(&mut item.attrs)? {
                Some(args) => {
                    let ItemStruct { generics, .. } = &item;
                    ensure_no_generics(generics)?;
                    JavaBinding::derive_struct(args.infer_docs, args.root_doc, item)?
                }
                None => continue,
            },
            Item::Enum(mut item) => match try_strip_args(&mut item.attrs)? {
                Some(args) => {
                    let ItemEnum { generics, .. } = &item;
                    ensure_no_generics(generics)?;
                    JavaBinding::derive_enum(args.infer_docs, args.root_doc, item)?
                }
                None => continue,
            },
            _ => {
                // ignore as the file may contain bindings for other libraries.
                continue;
            }
        };
        bindings.write(writer)?;
    }
    Ok(())
}

struct RustType {
    attrs: Vec<Attribute>,
    ident: Ident,
    fields: Fields,
}

fn ensure_no_generics(generics: &Generics) -> Result<(), syn::Error> {
    if generics.params.is_empty() {
        Ok(())
    } else {
        Err(syn::Error::new(
            generics.params.span(),
            "Generics are not supported",
        ))
    }
}

#[derive(Debug)]
pub struct DeriveArgs {
    pub infer_docs: bool,
    pub root_doc: Documentation,
}

impl DeriveArgs {
    fn add_doc(&mut self, str: String) {
        self.root_doc.push_line(str);
    }
}

impl Default for DeriveArgs {
    fn default() -> Self {
        DeriveArgs {
            infer_docs: true,
            root_doc: Documentation::from_style(FormatStyle::Documentation),
        }
    }
}
