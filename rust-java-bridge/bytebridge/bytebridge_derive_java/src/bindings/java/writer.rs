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

use crate::bindings::java::models::{JavaMethod, JavaType};
use crate::docs::{Documentation, FormatStyle};
use std::fs::{create_dir_all, File};
use std::io;
use std::io::{stdout, BufWriter, ErrorKind, Stdout, Write};
use std::path::{Path, PathBuf};

pub const INDENTATION: &str = "  ";

pub trait WriterFactory {
    fn writer_for(&mut self, package_name: String, file_name: &Path) -> io::Result<Writer>;
}

impl WriterFactory for Stdout {
    fn writer_for(&mut self, _package_name: String, file_name: &Path) -> io::Result<Writer> {
        let mut out = stdout();
        writeln!(out, ">>>>> File: {:?}.java", file_name)?;
        Ok(Writer::new(Box::new(out)))
    }
}

pub struct DirectoryWriter {
    path: PathBuf,
}

impl WriterFactory for DirectoryWriter {
    fn writer_for(&mut self, package_name: String, file_name: &Path) -> io::Result<Writer> {
        let mut file_path = self.path.clone();
        file_path.push(package_name.replace('.', "/"));
        create_dir_all(&file_path)?;

        file_path.push(file_name);
        assert!(file_path.set_extension("java"));

        Ok(Writer::new(Box::new(BufWriter::new(File::create(
            file_path,
        )?))))
    }
}

/// A Java file writer.
pub struct Writer {
    /// The inner writer.
    inner: Box<dyn Write + 'static>,
    /// The block depth.
    depth: usize,
}

impl Writer {
    fn new(inner: Box<dyn Write + 'static>) -> Writer {
        Writer { inner, depth: 0 }
    }

    /// Begins a new block.
    pub fn enter_block(&mut self) -> io::Result<()> {
        self.write(" {", false)?;
        self.depth += 1;
        Ok(())
    }

    /// Exists a block.
    pub fn exit_block(&mut self) -> io::Result<()> {
        self.depth -= 1;
        self.write_indented("}", true)?;
        Ok(())
    }

    /// Writes a new line.
    pub fn new_line(&mut self) -> io::Result<()> {
        self.new_lines(1)
    }

    /// Writes 'count' new lines.
    pub fn new_lines(&mut self, count: usize) -> io::Result<()> {
        for _ in 0..count {
            self.write("\n", false)?
        }
        Ok(())
    }

    /// Returns the current level of indentation.
    fn indentation(&self) -> String {
        (0..self.depth).fold(String::new(), |mut acc, _| {
            acc.push_str(INDENTATION);
            acc
        })
    }

    /// Writes a line and ignores the current indentation depth. Appends a new line if 'new_line' is
    /// true.
    pub fn write(&mut self, line: impl ToString, new_line: bool) -> io::Result<()> {
        let line_ending = if new_line { "\n" } else { "" };
        self.inner
            .write_all(format!("{}{}", line.to_string(), line_ending).as_bytes())
    }

    /// Writes a line. Appends a new line if 'new_line' is true.
    pub fn write_indented(&mut self, line: impl ToString, new_line: bool) -> io::Result<()> {
        let indentation = self.indentation();

        self.inner.write_all(indentation.as_bytes())?;
        self.inner.write_all(line.to_string().as_bytes())?;

        if new_line {
            self.inner.write_all(b"\n")?;
        }

        Ok(())
    }

    /// Writes all elements in the provided iterator. Appends a new line if 'new_line' is true.
    pub fn write_all_indented<I, S>(&mut self, lines: I, new_line: bool) -> io::Result<()>
    where
        I: Iterator<Item = S>,
        S: ToString,
    {
        let indentation = self.indentation();

        for line in lines {
            self.inner.write_all(indentation.as_bytes())?;
            self.inner
                .write_all(format!("{}\n", line.to_string()).as_bytes())?
        }

        if new_line {
            self.inner.write_all(b"\n")?;
        }

        Ok(())
    }
}

/// A Java source code writer builder. Provides control over where classes are written to, the
/// package name of the files and any copyright to apply to the derived classes.
pub struct JavaSourceWriterBuilder {
    writer: Box<dyn WriterFactory>,
    copyright: Option<Documentation>,
    package: String,
}

impl JavaSourceWriterBuilder {
    /// Returns a Java source writer builder that writes to the standard output stream.
    pub fn std_out(package: impl ToString) -> JavaSourceWriterBuilder {
        JavaSourceWriterBuilder {
            writer: Box::new(stdout()),
            copyright: None,
            package: package.to_string(),
        }
    }

    /// Returns a Java source writer builder that writes files to 'out_directory'.
    ///
    /// This will fail if 'out_directory' is a file or the directory does not exist.
    pub fn dir<P>(out_directory: P, package: impl ToString) -> io::Result<JavaSourceWriterBuilder>
    where
        P: AsRef<Path>,
    {
        let path = out_directory.as_ref();
        if !path.is_dir() {
            return Err(ErrorKind::NotFound.into());
        }

        Ok(JavaSourceWriterBuilder {
            writer: Box::new(DirectoryWriter {
                path: path.to_path_buf(),
            }),
            copyright: None,
            package: package.to_string(),
        })
    }

    /// Sets a copyright head that will be prepended to any source files that are written.
    pub fn copyright_header<C>(
        mut self,
        copyright_body: C,
        style: FormatStyle,
    ) -> JavaSourceWriterBuilder
    where
        C: Into<String>,
    {
        self.copyright = Some(Documentation::new(copyright_body.into(), style));
        self
    }

    /// Consumes this builder and returns an initialised Java Source Writer.
    pub fn build(self) -> JavaSourceWriter {
        let JavaSourceWriterBuilder {
            writer,
            copyright,
            package,
        } = self;
        JavaSourceWriter {
            writer,
            copyright: copyright
                .map(Documentation::build)
                .unwrap_or_else(|| "".to_string()),
            package,
        }
    }
}

pub struct JavaSourceWriter {
    writer: Box<dyn WriterFactory>,
    copyright: String,
    package: String,
}

impl JavaSourceWriter {
    pub fn for_file<P>(&mut self, name: P) -> io::Result<JavaFileWriter>
    where
        P: AsRef<Path>,
    {
        Ok(JavaFileWriter {
            writer: self
                .writer
                .writer_for(self.package.clone(), name.as_ref())?,
            copyright: self.copyright.clone(),
            package: self.package.clone(),
        })
    }
}

fn write_block(writer: &mut Writer, block: String) -> io::Result<()> {
    let indentation = writer.indentation();
    let mut iter = block.lines().peekable();

    loop {
        let have_next = iter.peek().is_some();
        match iter.next() {
            Some(line) => {
                writer.write(format!("{}{}", indentation, line), have_next)?;
            }
            None => break,
        }
    }

    Ok(())
}

pub enum ClassType {
    Abstract,
    Concrete,
    Subclass(String),
}

pub struct JavaFileWriter {
    writer: Writer,
    copyright: String,
    package: String,
}

impl JavaFileWriter {
    pub fn begin_class(
        self,
        name: impl ToString,
        documentation: Documentation,
        class_type: ClassType,
    ) -> io::Result<JavaClassWriter> {
        let JavaFileWriter {
            mut writer,
            copyright,
            package,
        } = self;

        write_block(&mut writer, copyright)?;

        if !documentation.is_empty() {
            write_block(&mut writer, documentation.build())?;
        }

        write_block(&mut writer, "/// THIS FILE IS AUTOMATICALLY GENERATED BY THE BYTE BRIDGE LIBRARY.\n/// ANY CHANGES MADE MAY BE LOST.".to_string())?;

        writer.write_indented(format!("package {};\n", package), true)?;
        writer.write_indented("import org.msgpack.core.MessagePacker;", true)?;
        writer.write_indented("import java.io.IOException;", true)?;
        writer.new_line()?;

        let declaration = match class_type {
            ClassType::Abstract => {
                format!("public abstract class {}", name.to_string())
            }
            ClassType::Concrete => {
                format!("public class {}", name.to_string())
            }
            ClassType::Subclass(superclass) => {
                format!("public class {} extends {}", name.to_string(), superclass)
            }
        };

        writer.write_indented(declaration, false)?;
        writer.enter_block()?;
        writer.new_lines(2)?;

        Ok(JavaClassWriter { writer })
    }
}

pub struct JavaClassWriter {
    writer: Writer,
}

impl JavaClassWriter {
    pub fn field(&mut self, ty: JavaType, default_value: String) -> JavaFieldWriter<'_> {
        JavaFieldWriter {
            writer: &mut self.writer,
            ty,
            documentation: Documentation::from_style(FormatStyle::Documentation),
            default_value,
        }
    }

    pub fn write_method(&mut self, method: JavaMethod) -> io::Result<()> {
        let JavaClassWriter { writer, .. } = self;
        let JavaMethod {
            name,
            documentation,
            return_type,
            args,
            r#abstract,
            body,
            annotation,
            throws,
        } = method;

        let args = args
            .into_iter()
            .map(|param| param.to_string())
            .collect::<Vec<_>>()
            .join(", ");

        writer.write_all_indented(documentation.build().lines(), false)?;
        if let Some(annotation) = annotation {
            writer.write_indented(annotation, true)?;
        }

        let modifier = if r#abstract { "abstract " } else { "" };
        let throws = if throws.is_empty() {
            "".to_string()
        } else {
            format!(" throws {}", throws.join(", "))
        };

        writer.write_indented(
            format!(
                "public {}{} {}({}){}",
                modifier, return_type, name, args, throws
            ),
            false,
        )?;

        if r#abstract && body.is_empty() {
            writer.write(";", true)?;
        } else {
            writer.enter_block()?;
            writer.new_line()?;
            body.write(writer)?;
            writer.exit_block()?;
        }
        writer.new_line()
    }

    pub fn end_class(&mut self) -> io::Result<()> {
        let JavaClassWriter { writer, .. } = self;
        writer.exit_block()
    }
}

pub struct JavaFieldWriter<'l> {
    writer: &'l mut Writer,
    ty: JavaType,
    documentation: Documentation,
    default_value: String,
}

impl<'l> JavaFieldWriter<'l> {
    pub fn set_documentation(mut self, documentation: Documentation) -> JavaFieldWriter<'l> {
        self.documentation = documentation;
        self
    }

    pub fn write(self, name: impl ToString) -> io::Result<()> {
        let JavaFieldWriter {
            writer,
            ty,
            documentation,
            default_value,
        } = self;

        writer.write_all_indented(documentation.build().lines(), false)?;
        writer.write_indented(
            format!("private {} {} = {};\n", ty, name.to_string(), default_value),
            true,
        )
    }
}
