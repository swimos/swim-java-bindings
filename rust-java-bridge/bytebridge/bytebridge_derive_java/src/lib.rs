mod bindings;
mod docs;
mod error;

use std::default::Default;
use std::path::PathBuf;

pub use docs::FormatStyle;
pub use error::Error;

use crate::bindings::generate_bindings;
pub use crate::bindings::{JavaSourceWriterBuilder, RustSourceWriterBuilder};

/// A builder for registering source files to derive a Java byte-level representation writer and
/// Rust byte reader for. This allows for efficient transport of classes over a JNI barrier.
#[derive(Debug, Default)]
pub struct Builder {
    input_files: Vec<PathBuf>,
}

impl Builder {
    /// Register a source file.
    pub fn add_source(mut self, path: PathBuf) -> Builder {
        self.input_files.push(path);
        self
    }

    /// Attempt to generate bindings for the provided sources and if they are valid them write them
    /// using the provided writers.
    pub fn generate(
        self,
        java_writer: JavaSourceWriterBuilder,
        rust_writer: RustSourceWriterBuilder,
    ) -> Result<(), Error> {
        let Builder { input_files } = self;
        generate_bindings(input_files, java_writer.build(), rust_writer.build())
    }
}
