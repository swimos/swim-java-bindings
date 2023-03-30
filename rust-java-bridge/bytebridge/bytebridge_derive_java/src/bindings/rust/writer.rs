use crate::docs::Documentation;
use crate::FormatStyle;
use quote::ToTokens;
use std::fs::File;
use std::io;
use std::io::{BufWriter, ErrorKind, Write};
use std::path::{Path, PathBuf};
use std::process::Command;
use syn::ItemStruct;

pub struct RustSourceWriterBuilder {
    writer: BufWriter<File>,
    path: PathBuf,
    format: bool,
    copyright: Option<Documentation>,
}

impl RustSourceWriterBuilder {
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

    pub fn format(mut self, format: bool) -> RustSourceWriterBuilder {
        self.format = format;
        self
    }

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
            copyright: copyright
                .map(Documentation::build)
                .unwrap_or("".to_string()),
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
        write!(self.writer, "{}", item.into_token_stream())
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
