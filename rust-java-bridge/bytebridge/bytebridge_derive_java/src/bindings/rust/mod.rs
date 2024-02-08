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
        let Variant { attrs, fields, .. } = i;
        self.visit_fields_mut(fields);
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
        write!(f, "{}", self.source.to_token_stream())
    }
}

impl RustBindings {
    /// Strips #[bytebridge] annotations from the item.
    pub fn build(mut item: Item) -> RustBindings {
        let mut builder = RustBindingsBuilder;
        builder.visit_item_mut(&mut item);
        RustBindings { source: item }
    }

    /// Writes the Rust bindings for the transformation into 'rust_writer'. This is delegated to the
    /// bytebridge_derive attribute macro.
    pub fn write(self, rust_writer: &mut RustSourceWriter) -> io::Result<()> {
        let RustBindings { source } = self;
        let tokens = derive_internals::derive(source.to_token_stream(), true);
        rust_writer.write_tokens(quote! {
            #source
            #tokens
        })
    }
}
