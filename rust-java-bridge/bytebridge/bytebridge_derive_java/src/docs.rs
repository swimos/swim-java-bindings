use std::path::PathBuf;
use std::{fs, io};

#[derive(Default, Debug, Clone)]
struct StringStack {
    inner: String,
}

impl StringStack {
    fn is_empty(&self) -> bool {
        self.inner.is_empty()
    }

    fn push_line(&mut self, line: impl ToString) {
        let line = line.to_string();
        let prefix = if self.is_empty() { "" } else { "\n" };

        if !line.starts_with(' ') {
            self.inner
                .push_str(&format!("{} {}", prefix, line));
        } else {
            self.inner
                .push_str(&format!("{}{}", prefix, line));
        }
    }
}

impl From<&str> for StringStack {
    fn from(s: &str) -> Self {
        StringStack::from(s.to_string())
    }
}

impl From<String> for StringStack {
    fn from(inner: String) -> Self {
        StringStack { inner }
    }
}

/// A Java documentation builder.
#[derive(Debug, Clone)]
pub struct Documentation {
    header: StringStack,
    params: StringStack,
    throws: StringStack,
    returns: Option<StringStack>,
    style: FormatStyle,
}

impl TryFrom<PathBuf> for Documentation {
    type Error = io::Error;

    fn try_from(value: PathBuf) -> Result<Self, Self::Error> {
        Ok(Documentation {
            header: StringStack::from(fs::read_to_string(value)?),
            params: Default::default(),
            throws: Default::default(),
            returns: None,
            style: FormatStyle::Documentation,
        })
    }
}

impl Documentation {
    pub fn new(header: String, style: FormatStyle) -> Documentation {
        Documentation {
            header: StringStack::from(header),
            params: Default::default(),
            throws: Default::default(),
            returns: None,
            style,
        }
    }

    pub fn empty_documentation() -> Documentation {
        Documentation::from_style(FormatStyle::Documentation)
    }

    pub fn from_style(style: FormatStyle) -> Documentation {
        Documentation {
            header: StringStack::default(),
            params: StringStack::default(),
            throws: StringStack::default(),
            returns: None,
            style,
        }
    }

    pub fn for_getter(name: String, default_value: String) -> Documentation {
        let mut inner = Documentation::from_style(FormatStyle::Documentation);

        inner.push_header_line(format!("Gets {}.", name));
        inner.push_header_line("<p>");
        inner.push_header_line(format!(" Default value: {}.", default_value));
        inner.set_returns(name);

        inner
    }

    pub fn for_setter(name: String) -> Documentation {
        let mut inner = Documentation::from_style(FormatStyle::Documentation);

        inner.push_header_line(format!("Sets the new {}.", name));
        inner.add_param(&name, format!("the new {}", name));

        inner
    }

    pub fn is_empty(&self) -> bool {
        let Documentation {
            header,
            params,
            throws,
            returns,
            ..
        } = self;
        header.is_empty()
            && params.is_empty()
            && throws.is_empty()
            && returns.as_ref().map(StringStack::is_empty).unwrap_or(true)
    }

    pub fn push_header_line(&mut self, line: impl ToString) {
        self.header.push_line(line)
    }

    pub fn add_param(&mut self, name: impl ToString, doc: impl ToString) {
        self.params
            .push_line(format!("@param {} {}", name.to_string(), doc.to_string()))
    }

    pub fn add_throws(&mut self, type_name: impl ToString, explanation: impl ToString) {
        self.throws.push_line(format!(
            "@throws {} {}",
            type_name.to_string(),
            explanation.to_string()
        ))
    }

    pub fn set_returns(&mut self, doc: impl ToString) {
        match self.returns {
            Some(_) => {
                panic!("Bug: attempted to set documentation for a return value twice")
            }
            None => {
                let mut stack = StringStack::default();
                stack.push_line(format!("@return {}", doc.to_string()));
                self.returns = Some(stack)
            }
        }
    }

    pub fn build(self) -> String {
        let Documentation {
            header,
            params,
            throws,
            returns,
            style,
        } = self;

        let mut block = header.inner;

        let term = !params.is_empty() || !throws.is_empty() || returns.is_some();
        if term {
            block = format!("{}\n\n", block);
        }

        if !params.is_empty() {
            block = format!("{}{}\n", block, params.inner);
        }

        if !throws.is_empty() {
            block = format!("{}{}", block, throws.inner);
        }

        if let Some(returns) = returns {
            if !returns.is_empty() {
                block = format!("{}{}", block, returns.inner);
            }
        }

        fn write_block(start: &'static str, inner: String) -> String {
            if inner.is_empty() {
                return String::default();
            }

            let mut buf = String::new();
            buf.push_str(&format!("{}\n", start));

            for line in inner.lines() {
                buf.push_str(&format!(" *{}\n", line));
            }
            buf.push_str(" */");
            buf
        }

        match style {
            FormatStyle::Block => write_block("/*", block),
            FormatStyle::Line => {
                let mut buf = String::new();
                for line in block.lines() {
                    buf.push_str(&format!("// {}\n", line));
                }
                buf
            }
            FormatStyle::Documentation => write_block("/**", block),
        }
    }
}

/// A format style for documentation.
#[derive(Debug, Copy, Clone)]
pub enum FormatStyle {
    /// Block style. E.g,
    /// ```
    /// /*
    ///  *
    ///  *
    ///  */
    /// ```
    Block,
    /// Line style. E.g,
    /// ```
    /// //
    /// //
    /// //
    /// //
    /// //
    /// ```
    Line,
    /// Documentation style. E.g,
    /// ```
    /// /**
    ///  *
    ///  *
    ///  */
    /// ```
    Documentation,
}
