use crate::bindings::java::models::{JavaMethod, JavaType};
use crate::docs::{Documentation, FormatStyle};
use std::fmt::Arguments;
use std::fs::File;
use std::io;
use std::io::{BufWriter, ErrorKind, Stdout, StdoutLock, Write};
use std::path::{Path, PathBuf};
use std::rc::Rc;

pub enum WriterFactory {
    StdOut,
    Dir { path: PathBuf },
}

impl WriterFactory {
    pub fn new_file(&mut self, file_name: &Path) -> io::Result<Writer> {
        match self {
            WriterFactory::StdOut => {
                let mut out = io::stdout();
                write!(out, ">>>>> File: {:?}.java\n", file_name)?;
                let mut writer = Writer::new_std_out(out, 1);
                Ok(writer)
            }
            WriterFactory::Dir { path } => {
                let mut file_path = path.clone();
                file_path.push(file_name);
                assert!(file_path.set_extension("java"));
                Ok(Writer::file(BufWriter::new(File::create(file_path)?)))
            }
        }
    }
}

pub struct Writer {
    inner: WriterHandle,
    depth: usize,
}

pub enum WriterHandle {
    StdOut(Stdout),
    File(Box<dyn Write + 'static>),
}

impl Writer {
    pub fn new_std_out(handle: Stdout, depth: usize) -> Writer {
        Writer {
            inner: WriterHandle::StdOut(handle),
            depth,
        }
    }

    pub fn std_out(handle: Stdout) -> Writer {
        Writer {
            inner: WriterHandle::StdOut(handle),
            depth: 0,
        }
    }

    pub fn file(handle: impl Write + 'static) -> Writer {
        Writer {
            inner: WriterHandle::File(Box::new(handle)),
            depth: 0,
        }
    }

    pub fn enter_block(&mut self) -> io::Result<()> {
        self.write(" {", false)?;
        self.depth += 1;
        Ok(())
    }

    pub fn exit_block(&mut self) -> io::Result<()> {
        self.depth -= 1;
        self.write_indented("}", true)?;
        assert!(self.depth >= 0);
        Ok(())
    }

    pub fn new_line(&mut self) -> io::Result<()> {
        self.new_lines(1)
    }

    pub fn new_lines(&mut self, count: usize) -> io::Result<()> {
        for _ in 0..count {
            self.write("\n", false)?
        }
        Ok(())
    }

    fn indentation(&self) -> String {
        (0..self.depth).fold(String::new(), |mut acc, _| {
            acc.push_str("  ");
            acc
        })
    }

    pub fn write(&mut self, line: impl ToString, new_line: bool) -> io::Result<()> {
        let line_ending = if new_line { "\n" } else { "" };
        self.inner
            .write_all(format!("{}{}", line.to_string(), line_ending).as_bytes())
    }

    pub fn write_indented(&mut self, line: impl ToString, new_line: bool) -> io::Result<()> {
        let indentation = self.indentation();

        self.inner.write_all(indentation.as_bytes())?;
        self.inner.write_all(line.to_string().as_bytes())?;

        if new_line {
            self.inner.write_all(b"\n")?;
        }

        Ok(())
    }

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

impl Write for WriterHandle {
    fn write(&mut self, buf: &[u8]) -> io::Result<usize> {
        match self {
            WriterHandle::StdOut(writer) => writer.write(buf),
            WriterHandle::File(writer) => writer.write(buf),
        }
    }

    fn flush(&mut self) -> io::Result<()> {
        match self {
            WriterHandle::StdOut(writer) => writer.flush(),
            WriterHandle::File(writer) => writer.flush(),
        }
    }
}

pub struct JavaSourceWriterBuilder {
    writer: WriterFactory,
    copyright: Option<Documentation>,
    max_line_length: usize,
    format: bool,
    package: String,
}

impl JavaSourceWriterBuilder {
    pub fn std_out(package: impl ToString) -> JavaSourceWriterBuilder {
        JavaSourceWriterBuilder {
            writer: WriterFactory::StdOut,
            copyright: None,
            max_line_length: 100,
            format: false,
            package: package.to_string(),
        }
    }

    pub fn dir<P>(out_directory: P, package: String) -> io::Result<JavaSourceWriterBuilder>
    where
        P: AsRef<Path>,
    {
        let path = out_directory.as_ref();
        if !path.is_dir() {
            return Err(ErrorKind::NotFound.into());
        }

        Ok(JavaSourceWriterBuilder {
            writer: WriterFactory::Dir {
                path: path.to_path_buf(),
            },
            copyright: None,
            max_line_length: 100,
            format: false,
            package,
        })
    }

    pub fn max_line_length(mut self, max_line_length: usize) -> JavaSourceWriterBuilder {
        self.max_line_length = max_line_length;
        self
    }

    pub fn format(mut self, format: bool) -> JavaSourceWriterBuilder {
        self.format = format;
        self
    }

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

    pub fn build(self) -> JavaSourceWriter {
        let JavaSourceWriterBuilder {
            writer,
            copyright,
            max_line_length,
            format,
            package,
        } = self;
        JavaSourceWriter {
            writer,
            copyright: copyright
                .map(Documentation::build)
                .unwrap_or("".to_string()),
            max_line_length,
            format,
            package,
        }
    }
}

pub struct JavaSourceWriter {
    writer: WriterFactory,
    copyright: String,
    max_line_length: usize,
    format: bool,
    package: String,
}

impl JavaSourceWriter {
    pub fn for_file<P>(&mut self, name: P) -> io::Result<JavaFileWriter>
    where
        P: AsRef<Path>,
    {
        Ok(JavaFileWriter {
            writer: self.writer.new_file(name.as_ref())?,
            copyright: self.copyright.clone(),
            package: self.package.clone(),
        })
    }
}

pub struct JavaFileWriter {
    writer: Writer,
    copyright: String,
    package: String,
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

impl JavaFileWriter {
    pub fn begin_class(
        self,
        name: impl ToString,
        documentation: Documentation,
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

        writer.write_indented(format!("package {};", package), true)?;

        writer.write_indented(format!("public class {}", name.to_string()), false)?;
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
            body,
            modifiers,
        } = method;

        let args = args
            .into_iter()
            .map(|param| param.to_string())
            .collect::<Vec<_>>()
            .join(", ");

        writer.write_all_indented(documentation.build().lines(), false)?;
        modifiers.write(writer)?;
        writer.write(format!(" {} {}({})", return_type, name, args), false)?;
        writer.enter_block()?;
        writer.new_line()?;
        body.write(writer)?;
        writer.exit_block()?;
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
    pub fn set_documentation(mut self, documentation: String) -> JavaFieldWriter<'l> {
        self.documentation = self.documentation.set_content(documentation);
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
