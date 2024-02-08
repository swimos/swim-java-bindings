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
