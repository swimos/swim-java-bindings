#![allow(warnings)]

mod bindings;
mod docs;
mod error;

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

pub use docs::FormatStyle;
pub use error::Error;

use crate::bindings::generate_bindings;
pub use crate::bindings::{JavaSourceWriterBuilder, RustSourceWriterBuilder};

#[derive(Debug, Default)]
pub struct Builder {
    input_files: Vec<PathBuf>,
}

impl Builder {
    pub fn add_source(mut self, path: PathBuf) -> Builder {
        self.input_files.push(path);
        self
    }

    pub fn generate(
        self,
        java_writer: JavaSourceWriterBuilder,
        rust_writer: RustSourceWriterBuilder,
    ) -> Result<(), Error> {
        let Builder { input_files } = self;
        generate_bindings(input_files, java_writer.build(), rust_writer.build())
    }
}
