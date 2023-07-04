# Swim Client cookbooks

This directory contains projects that demonstrate various features of the Swim Client library.

## Setup

The Swim Java library is a wrapper around the Swim Rust implementation and as such it requires that
the [Rust programming language](https://www.rust-lang.org/) is installed and that the Swim Rust Git submodule in this
repository has been checked out. This can be done by executing the following command from
its [directory](../../swim-rust):

``
git submodule update --init --recursive
``

Following both the Rust programming language being installed and the Swim Rust submodule being initialised, the
cookbooks will automatically build the native library and add the native dependencies to the JAR file when they are
built.

To run an example, run the basic plane and wait for it to start and then run the corresponding custom client.

## Cookbooks

- [Value Downlinks](value_downlink): a client example that demonstrates communicating with a Value Lane using a Value
  Downlink.
- [Map Downlinks](map_downlink): a client example that demonstrates communicating with a Map Lane using a Map Downlink.