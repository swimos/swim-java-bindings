use bytebridge::{FormatStyle, JavaSourceWriterBuilder, RustSourceWriterBuilder};
use std::env;
use std::env::current_dir;
use std::path::{Path, PathBuf};

fn main() {
    let models = Path::new("src").join("ffi_models.rs");
    println!("cargo:rerun-if-changed={}", models.display());

    let mut build_file: PathBuf = env::var("OUT_DIR").unwrap().parse().unwrap();
    build_file.push("out.rs");

    let builder = bytebridge::Builder::default();
    let _java_writer = JavaSourceWriterBuilder::std_out("test.package.somewhere").copyright_header(
        r#"Copyright line 1
Copyright line 2"#,
        FormatStyle::Line,
    );

    let java_writer = JavaSourceWriterBuilder::dir(current_dir().unwrap(), "ai.swim").unwrap();

    let rust_writer = RustSourceWriterBuilder::file(build_file)
        .unwrap()
        .format(true);
    builder
        .add_source(models)
        .generate(java_writer, rust_writer)
        .unwrap();
}
