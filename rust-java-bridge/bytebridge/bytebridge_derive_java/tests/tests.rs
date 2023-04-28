use bytebridge_derive_java::{Builder, Error, JavaSourceWriterBuilder, RustSourceWriterBuilder};
use quote::ToTokens;
use std::env::current_dir;
use std::fs::{read_dir, read_to_string, DirEntry, File};
use std::io::Write;
use std::path::PathBuf;
use syn::punctuated::Punctuated;
use syn::{Attribute, Expr, ExprLit, Item, Lit, Meta, Token};

enum TestCase {
    Class {
        name: String,
        input: String,
    },
    Abstract {
        superclass_name: String,
        input: String,
        variants: Vec<String>,
    },
}

impl TestCase {
    fn build_from(input: impl ToTokens) -> TestCase {
        let input = input.to_token_stream();
        let input = syn::parse2::<Item>(input.to_token_stream()).expect("Read invalid tokens");
        match input {
            Item::Struct(item) => TestCase::Class {
                name: unpack_rename(&item.attrs).unwrap_or_else(|| item.ident.to_string()),
                input: item.to_token_stream().to_string(),
            },
            Item::Enum(item) => {
                let superclass_name =
                    unpack_rename(&item.attrs).unwrap_or_else(|| item.ident.to_string());
                TestCase::Abstract {
                    superclass_name: superclass_name.clone(),
                    input: item.to_token_stream().to_string(),
                    variants: item
                        .variants
                        .iter()
                        .map(|variant| {
                            unpack_rename(&variant.attrs).unwrap_or_else(|| {
                                format!("{}{}", superclass_name, variant.ident)
                            })
                        })
                        .collect(),
                }
            }
            i => {
                panic!("Expected an enum or struct. Received: {:?}", i)
            }
        }
    }

    fn write_into(&self, file: &mut File) {
        match self {
            TestCase::Class { input, .. } => file
                .write_all(input.as_bytes())
                .expect("Failed to write test input"),
            TestCase::Abstract { input, .. } => file
                .write_all(input.as_bytes())
                .expect("Failed to write test input"),
        }
    }

    fn take_output(self, working_dir: &mut PathBuf) -> TestOutput {
        match self {
            TestCase::Class { name, .. } => {
                working_dir.push(format!("ai/swim/{}.java", name));
                TestOutput::Class {
                    content: read_to_string(working_dir).expect("Failed to read test file"),
                }
            }
            TestCase::Abstract {
                superclass_name,
                variants,
                ..
            } => {
                let superclass_content = {
                    let mut working_dir = working_dir.clone();
                    working_dir.push(format!("ai/swim/{}.java", superclass_name));
                    read_to_string(working_dir).expect("Failed to read test file")
                };

                let subclasses =
                    variants
                        .into_iter()
                        .fold(Vec::new(), |mut subclasses, class_name| {
                            let mut working_dir = working_dir.clone();
                            working_dir.push("ai/swim");
                            working_dir.push(&class_name);
                            assert!(working_dir.set_extension("java"));
                            subclasses.push((
                                class_name,
                                read_to_string(working_dir).expect("Failed to read test file"),
                            ));
                            subclasses
                        });
                TestOutput::AbstractClass {
                    superclass_name,
                    superclass_content,
                    subclasses,
                }
            }
        }
    }
}

fn unpack_rename(attrs: &[Attribute]) -> Option<String> {
    for attr in attrs {
        if !attr.path().is_ident("bytebridge") {
            continue;
        } else if attr.meta.require_list().is_err() {
            continue;
        }

        let nested = attr
            .parse_args_with(Punctuated::<Meta, Token![,]>::parse_terminated)
            .expect("Invalid attribute");
        for nested in nested {
            match nested {
                Meta::NameValue(meta) if meta.path.is_ident("rename") => match meta.value {
                    Expr::Lit(ExprLit {
                        lit: Lit::Str(str), ..
                    }) => {
                        return Some(str.value());
                    }
                    _ => {}
                },
                _ => {}
            }
        }
    }

    None
}

#[derive(Debug)]
enum TestOutput {
    Class {
        content: String,
    },
    AbstractClass {
        superclass_name: String,
        superclass_content: String,
        subclasses: Vec<(String, String)>,
    },
}

fn run_test<F>(input: impl ToTokens, assertion: F)
where
    F: FnOnce(Result<TestOutput, Error>),
{
    let dir = tempfile::tempdir().expect("Failed to create test directory");

    // Rust token input setup
    let mut source_path = dir.path().to_path_buf();
    source_path.push("models.rs");
    let mut source_file = File::create(&source_path).expect("Failed to create model file");

    let test_case = TestCase::build_from(input);
    test_case.write_into(&mut source_file);

    // Rust output setup
    let mut build_path = dir.path().to_path_buf();
    build_path.push("output.rs");

    let builder = Builder::default();
    let java_writer = JavaSourceWriterBuilder::dir(&dir, "ai.swim").unwrap();
    let rust_writer = RustSourceWriterBuilder::file(build_path)
        .unwrap()
        .format(true);

    let result = builder
        .add_source(source_path)
        .generate(java_writer, rust_writer);
    match result {
        Ok(()) => assertion(Ok(test_case.take_output(&mut dir.into_path()))),
        Err(e) => assertion(Err(e)),
    }
}

fn run_test_ok<F>(input: impl ToTokens, assertion: F)
where
    F: FnOnce(TestOutput),
{
    run_test(input, |result| match result {
        Ok(output) => assertion(output),
        Err(e) => panic!("Expected test to pass: {:?}", e),
    })
}

fn run_test_err<F>(input: impl ToTokens, assertion: F)
where
    F: FnOnce(Error),
{
    run_test(input, |result| match result {
        Ok(output) => panic!("Expected test to pass:\n{:?}", output),
        Err(e) => assertion(e),
    })
}

fn dir_iter<F>(suffix: &str, with: F)
where
    F: Fn(DirEntry),
{
    let mut pwd = current_dir().expect("Failed to get current directory");
    pwd.push(suffix);

    let input_files = read_dir(pwd).expect("Failed to get tests");

    for path in input_files {
        let entry = path.expect("Failed to read entry");
        with(entry);
    }
}

#[test]
fn concrete_classes() {
    let mut outputs = current_dir().expect("Failed to get current directory");
    outputs.push("tests/concrete/outputs");

    dir_iter("tests/concrete/inputs", |entry| {
        let file_type = entry.file_type().expect("Failed to read file type");
        if file_type.is_dir() {
            panic!("Invalid file structure. Received a directory: {:?}", entry);
        }

        if entry.file_name() == ".DS_Store" {
            return;
        }

        println!("Running test -> {:?}", entry.path());

        let file =
            syn::parse_file(&read_to_string(entry.path()).expect("Failed to read file contents"))
                .expect("File contains invalid tokens");

        run_test_ok(file, |output| {
            let file = PathBuf::from(entry.file_name());
            let file_name = file.file_stem().expect("Invalid file name");
            let mut output_file = outputs.clone();
            output_file.push(file_name);
            output_file.set_extension("java");

            match output_file.try_exists() {
                Ok(true) => {
                    let file_content =
                        read_to_string(output_file).expect("Failed to load output file");
                    expect_class(output, file_content)
                }
                e => panic!("Missing file ({:?}) -> {:?}", e, file),
            }
        });
    });
}

fn assert_file(mut base: PathBuf, name: String, expected: String) {
    base.push(name);
    base.set_extension("java");

    let expected_superclass_content = read_to_string(base).expect("Failed to load output file");

    assert_eq!(expected, expected_superclass_content);
}

fn expect_class(output: TestOutput, expected_content: String) {
    match output {
        TestOutput::Class { content } => {
            assert_eq!(content, expected_content.as_str())
        }
        output @ TestOutput::AbstractClass { .. } => {
            panic!("Expected a class. Got: {:?}", output)
        }
    }
}

#[test]
fn abstract_classes() {
    let mut outputs = current_dir().expect("Failed to get current directory");
    outputs.push("tests/abstract/outputs");

    dir_iter("tests/abstract/inputs", |entry| {
        if entry.file_name() == ".DS_Store" {
            return;
        }

        let file_type = entry.file_type().expect("Failed to read file type");
        if file_type.is_dir() {
            panic!("Invalid file structure. Received a directory: {:?}", entry);
        }

        println!("Running test -> {:?}", entry.path());

        let file =
            syn::parse_file(&read_to_string(entry.path()).expect("Failed to read file contents"))
                .expect("File contains invalid tokens");

        let entry_file = PathBuf::from(entry.file_name());
        let file_name = entry_file.file_stem().expect("Invalid file name");
        let mut outputs = outputs.clone();
        outputs.push(file_name);

        run_test_ok(file, |output| match output {
            t @ TestOutput::Class { .. } => {
                panic!("Expected an abstract class. Got: {:?}", t)
            }
            TestOutput::AbstractClass {
                superclass_name,
                superclass_content,
                subclasses,
            } => {
                assert_file(outputs.clone(), superclass_name, superclass_content);

                for (name, content) in subclasses {
                    assert_file(outputs.clone(), name, content);
                }
            }
        });
    });
}
