use bytebridge::{FormatStyle, JavaSourceWriterBuilder};
use std::path::Path;

fn main() {
    let models = Path::new("src").join("models.rs");

    println!("cargo:rerun-if-changed={}", models.display());

    let builder = bytebridge::Builder::default();
    let java_writer = JavaSourceWriterBuilder::std_out().copyright_header(
        r#"Copyright line 1
Copyright line 2"#,
        FormatStyle::Line,
    );
    builder.add_source(models).generate(java_writer).unwrap();
}
