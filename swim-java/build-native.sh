#!/bin/bash

./gradlew jar
"$GRAALVM_HOME"/bin/native-image -jar build/libs/swim-java-rust-1.0-SNAPSHOT.jar
./swim-java-rust-1.0-SNAPSHOT -Djava.library.path=../rustffi/target/release/