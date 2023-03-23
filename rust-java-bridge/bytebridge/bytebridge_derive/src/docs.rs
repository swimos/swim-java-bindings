use heck::{AsTitleCase, ToTitleCase};
use std::path::PathBuf;
use std::{fs, io};

#[derive(Debug, Clone)]
pub struct Documentation {
    inner: String,
    style: FormatStyle,
}

impl TryFrom<PathBuf> for Documentation {
    type Error = io::Error;

    fn try_from(value: PathBuf) -> Result<Self, Self::Error> {
        Ok(Documentation {
            inner: fs::read_to_string(value)?,
            style: FormatStyle::Documentation,
        })
    }
}

pub struct GetterDocumentationBuilder {
    name: String,
    stack: String,
    inner: Documentation,
    default: Option<String>,
}

impl GetterDocumentationBuilder {
    pub fn set_default(mut self, to: String) -> GetterDocumentationBuilder {
        self.default = Some(to);
        self
    }

    pub fn push_lines(mut self, line: String) -> GetterDocumentationBuilder {
        self.stack.push_str(line.as_str());
        self
    }

    pub fn build(self) -> Documentation {
        let GetterDocumentationBuilder {
            name,
            stack,
            mut inner,
            default,
        } = self;

        inner.push_line(format!("Gets {}.", name));
        inner.push_line(stack);

        if let Some(default) = default {
            inner.push_line(format!("Default value: {}.", default));
        }

        inner.push_line(format!(
            "@return the {}",
            name.to_title_case().to_lowercase()
        ));

        inner
    }
}

pub struct SetterDocumentationBuilder {
    name: String,
    stack: String,
    inner: Documentation,
}

impl SetterDocumentationBuilder {
    pub fn build(self) -> Documentation {
        let SetterDocumentationBuilder {
            name,
            stack,
            mut inner,
        } = self;

        inner.push_line(format!("Sets the new {}.", name));
        inner.push_line(stack);
        inner.push_line(format!("@param {} the new {}", name, name));

        inner
    }
}

impl Documentation {
    pub fn empty() -> Documentation {
        Documentation {
            inner: String::new(),
            style: FormatStyle::Documentation,
        }
    }

    pub fn new(inner: String, style: FormatStyle) -> Documentation {
        Documentation { inner, style }
    }

    pub fn from_style(style: FormatStyle) -> Documentation {
        Documentation {
            inner: String::default(),
            style,
        }
    }

    pub fn getter(name: impl ToString) -> GetterDocumentationBuilder {
        GetterDocumentationBuilder {
            name: name.to_string(),
            stack: String::new(),
            inner: Documentation::from_style(FormatStyle::Documentation),
            default: None,
        }
    }

    pub fn setter(name: impl ToString) -> SetterDocumentationBuilder {
        SetterDocumentationBuilder {
            name: name.to_string(),
            stack: String::new(),
            inner: Documentation::from_style(FormatStyle::Documentation),
        }
    }

    pub fn set_content(mut self, to: String) -> Documentation {
        self.inner = to;
        self
    }

    pub fn is_empty(&self) -> bool {
        self.inner.is_empty()
    }

    pub fn content(&self) -> String {
        self.inner.clone()
    }

    pub fn push_line(&mut self, line: String) {
        if self.inner.is_empty() {
            self.inner.push_str(line.as_str());
        } else {
            self.inner.push_str(&format!("\n{}", line));
        }
    }

    pub fn build(self) -> String {
        let Documentation { inner, style } = self;

        fn write_block(start: &'static str, inner: String) -> String {
            if inner.is_empty() {
                return String::default();
            }

            let mut buf = String::new();
            buf.push_str(&format!("{}\n", start));

            for line in inner.lines() {
                buf.push_str(&format!(" * {}\n", line));
            }
            buf.push_str(&format!(" */"));
            buf
        }

        match style {
            FormatStyle::Block => write_block("/*", inner),
            FormatStyle::Line => {
                let mut buf = String::new();
                for line in inner.lines() {
                    buf.push_str(&format!("// {}\n", line));
                }
                buf
            }
            FormatStyle::Documentation => write_block("/**", inner),
        }
    }
}

impl From<String> for Documentation {
    fn from(value: String) -> Self {
        value.as_str().into()
    }
}

impl From<&str> for Documentation {
    fn from(value: &str) -> Self {
        Documentation {
            inner: value.to_string(),
            style: FormatStyle::Documentation,
        }
    }
}

#[derive(Debug, Copy, Clone)]
pub enum FormatStyle {
    Block,
    Line,
    Documentation,
}
