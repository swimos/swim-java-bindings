mod writer;

use quote::{quote, ToTokens};
use std::fmt::{Debug, Formatter};
use std::io;
pub use writer::{RustSourceWriter, RustSourceWriterBuilder};

use crate::bindings::MACRO_PATH;
use syn::visit_mut::VisitMut;
use syn::{Attribute, Field, Item, ItemEnum, ItemStruct, Variant};

struct RustBindingsBuilder;

impl VisitMut for RustBindingsBuilder {
    fn visit_field_mut(&mut self, i: &mut Field) {
        strip_attributes(&mut i.attrs);
    }

    fn visit_item_enum_mut(&mut self, i: &mut ItemEnum) {
        let ItemEnum {
            attrs, variants, ..
        } = i;

        for variant in variants {
            self.visit_variant_mut(variant)
        }

        strip_attributes(attrs);
    }

    fn visit_item_struct_mut(&mut self, i: &mut ItemStruct) {
        let ItemStruct { attrs, fields, .. } = i;
        self.visit_fields_mut(fields);
        strip_attributes(attrs);
    }

    fn visit_variant_mut(&mut self, i: &mut Variant) {
        let Variant { attrs, .. } = i;
        strip_attributes(attrs);
    }
}

fn strip_attributes(attrs: &mut Vec<Attribute>) {
    attrs.retain(|attr| !attr.path().is_ident(MACRO_PATH));
}

pub struct RustBindings {
    source: Item,
}

impl Debug for RustBindings {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.source.to_token_stream().to_string())
    }
}

impl RustBindings {
    pub fn build(mut item: Item) -> RustBindings {
        let mut builder = RustBindingsBuilder;
        builder.visit_item_mut(&mut item);
        RustBindings { source: item }
    }

    pub fn write(self, rust_writer: &mut RustSourceWriter) -> io::Result<()> {
        let RustBindings { source } = self;

        let tokens = derive_internals::derive(source.to_token_stream());
        rust_writer.write_tokens(quote! {
            #source
            #tokens
        })
    }
}
