use bytebridge::{JavaSourceWriterBuilder, RustSourceWriterBuilder};
use std::env;
use std::env::current_dir;
use std::fs::create_dir_all;
use std::path::{Path, PathBuf};

fn main() {
    let models = Path::new("src").join("ffi_models.rs");
    println!("cargo:rerun-if-changed={}", models.display());

    let mut build_file: PathBuf = env::var("OUT_DIR").unwrap().parse().unwrap();
    build_file.push("out.rs");

    let builder = bytebridge::Builder::default();

    let mut dir = current_dir().unwrap();
    dir.push("../../swim-java/swim-client/build/generated/main/java");

    if !dir.exists() {
        create_dir_all(&dir).unwrap();
    }

    let java_writer = JavaSourceWriterBuilder::dir(dir, "ai.swim.client").unwrap();

    let rust_writer = RustSourceWriterBuilder::file(build_file)
        .unwrap()
        .format(true);
    builder
        .add_source(models)
        .generate(java_writer, rust_writer)
        .unwrap();
}
