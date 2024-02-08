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
