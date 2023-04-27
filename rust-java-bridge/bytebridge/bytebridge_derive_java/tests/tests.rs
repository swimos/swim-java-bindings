use bytebridge_derive_java::{Builder, Error, JavaSourceWriterBuilder, RustSourceWriterBuilder};
use quote::ToTokens;
use std::env::current_dir;
use std::fs::{read_dir, read_to_string, DirEntry, File};
use std::io::Write;
use std::path::PathBuf;
use syn::Item;

enum TestCase {
    Class {
        name: String,
        input: String,
    },
    Abstract {
        name: String,
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
                name: item.ident.to_string(),
                input: item.to_token_stream().to_string(),
            },
            Item::Enum(item) => {
                let superclass_name = item.ident.to_string();

                TestCase::Abstract {
                    name: superclass_name.clone(),
                    input: item.to_token_stream().to_string(),
                    variants: item
                        .variants
                        .iter()
                        .map(|variant| format!("{}{}", superclass_name, variant.ident.to_string()))
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
            TestCase::Abstract { name, variants, .. } => {
                let superclass_content = {
                    let mut working_dir = working_dir.clone();
                    working_dir.push(format!("ai/swim/{}.java", name));
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
                    superclass_content,
                    subclasses,
                }
            }
        }
    }
}

#[derive(Debug)]
enum TestOutput {
    Class {
        content: String,
    },
    AbstractClass {
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
                    match output {
                        TestOutput::Class { content } => {
                            assert_eq!(content, file_content.as_str())
                        }
                        output @ TestOutput::AbstractClass { .. } => {
                            panic!("Expected a class. Got: {:?}", output)
                        }
                    }
                }
                e => panic!("Missing file ({:?}) -> {:?}", e, file),
            }
        });
    });
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

        // entry.file_name();

        println!("Running test -> {:?}", entry.path());

        let file =
            syn::parse_file(&read_to_string(entry.path()).expect("Failed to read file contents"))
                .expect("File contains invalid tokens");

        run_test_ok(file, |output| {
            let mut output_file = outputs.clone();
            output_file.push(entry.file_name());

            match output {
                t @ TestOutput::Class { .. } => {
                    panic!("Expected an abstract class. Got: {:?}", t)
                }
                TestOutput::AbstractClass {
                    superclass_content,
                    subclasses,
                } => {
                    assert_eq!(content, superclass_content.as_str())
                }
            }

            let file = PathBuf::from(entry.file_name());
            let file_name = file.file_stem().expect("Invalid file name");
            let mut output_file = outputs.clone();
            output_file.push(file_name);
            output_file.set_extension("java");

            match output_file.try_exists() {
                Ok(true) => {
                    let file_content =
                        read_to_string(output_file).expect("Failed to load output file");
                    expect_class(output, file_content.as_str());
                }
                e => panic!("Missing file ({:?}) -> {:?}", e, file),
            }
        });
    });
}
