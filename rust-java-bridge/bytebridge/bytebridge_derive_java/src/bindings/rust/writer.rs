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

use crate::docs::Documentation;
use crate::FormatStyle;
use quote::ToTokens;
use std::fs::File;
use std::io;
use std::io::{BufWriter, Write};
use std::path::{Path, PathBuf};
use std::process::Command;

/// A Rust source code writer builder. Provides control over where the derived tokens are written
/// to, whether to invoke rustfmt and control over applying a copyright header.
pub struct RustSourceWriterBuilder {
    writer: BufWriter<File>,
    path: PathBuf,
    format: bool,
    copyright: Option<Documentation>,
}

impl RustSourceWriterBuilder {
    /// Returns a Rust source writer builder that writes to 'file'.
    ///
    /// This will fail if it's not possible to create 'file' and will truncate it if it already
    /// exists.
    pub fn file<P>(file: P) -> io::Result<RustSourceWriterBuilder>
    where
        P: AsRef<Path>,
    {
        let path = file.as_ref();
        Ok(RustSourceWriterBuilder {
            writer: BufWriter::new(File::create(path)?),
            path: path.to_path_buf(),
            copyright: None,
            format: false,
        })
    }

    /// Sets whether to invoke rustfmt on the derived tokens.
    pub fn format(mut self, format: bool) -> RustSourceWriterBuilder {
        self.format = format;
        self
    }

    /// Sets a copyright head that will be prepended to any source files that are written.
    pub fn copyright_header<C>(
        mut self,
        copyright_body: C,
        style: FormatStyle,
    ) -> RustSourceWriterBuilder
    where
        C: Into<String>,
    {
        self.copyright = Some(Documentation::new(copyright_body.into(), style));
        self
    }

    /// Consumes this builder and returns an initialised Rust Source Writer.
    pub fn build(self) -> RustSourceWriter {
        let RustSourceWriterBuilder {
            writer,
            path,
            format,
            copyright,
        } = self;
        RustSourceWriter {
            writer,
            path,
            format,
            copyright: copyright.map(Documentation::build).unwrap_or_default(),
        }
    }
}

pub struct RustSourceWriter {
    writer: BufWriter<File>,
    path: PathBuf,
    format: bool,
    copyright: String,
}

impl RustSourceWriter {
    pub fn write_tokens(&mut self, item: impl ToTokens) -> io::Result<()> {
        let RustSourceWriter {
            writer, copyright, ..
        } = self;

        if copyright.is_empty() {
            write!(writer, "{}\n{}", copyright, item.into_token_stream())
        } else {
            write!(writer, "{}", item.into_token_stream())
        }
    }

    pub fn complete(mut self) -> io::Result<()> {
        self.writer.flush()?;

        if self.format {
            let mut command = Command::new("rustfmt");
            command.arg(self.path);

            let mut child = command.spawn()?;
            child.wait()?;
        }
        Ok(())
    }
}
