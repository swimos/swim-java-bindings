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

use bytebridge::{FormatStyle, JavaSourceWriterBuilder, RustSourceWriterBuilder};
use std::env;
use std::path::{Path, PathBuf};

fn main() {
    let models = Path::new("src").join("models.rs");
    println!("cargo:rerun-if-changed={}", models.display());

    let mut build_file: PathBuf = env::var("OUT_DIR").unwrap().parse().unwrap();
    build_file.push("out.rs");

    let builder = bytebridge::Builder::default();
    let java_writer = JavaSourceWriterBuilder::std_out("test.package.somewhere").copyright_header(
        r#"Copyright line 1
Copyright line 2"#,
        FormatStyle::Line,
    );

    let rust_writer = RustSourceWriterBuilder::file(build_file)
        .unwrap()
        .format(true);
    builder
        .add_source(models)
        .generate(java_writer, rust_writer)
        .unwrap()
}
