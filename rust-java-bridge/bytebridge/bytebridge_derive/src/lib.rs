use proc_macro::TokenStream;

#[proc_macro_derive(ByteCodec)]
pub fn bytebridge(input: TokenStream) -> TokenStream {
    derive_internals::derive(input.into()).into()
}
