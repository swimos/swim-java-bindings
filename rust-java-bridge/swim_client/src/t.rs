use bytebridge::ByteCodec;
use bytes::BytesMut;

#[test]
fn t() {
    let bytes = vec![
        -102i8, -51, 32, 0, -51, 16, 0, 32, 32, 15, 15, -61, -61, -61, 1,
    ];
    let bytes = bytes.into_iter().map(|b| b as u8).collect::<Vec<_>>();

    let mut buf = BytesMut::from_iter(bytes);
    ClientConfig::try_from_bytes(&mut buf).unwrap();
}

#[derive(Debug, Default)]
pub struct ClientConfig {
    pub max_message_size: u32,
    pub remote_buffer_size: u32,
    pub transport_buffer_size: u32,
    pub registration_buffer_size: u32,
    #[doc = " The client's LZ77 sliding window size. Negotiated during the HTTP upgrade. In client mode,"]
    #[doc = " this conforms to RFC 7692 7.1.2.1. In server mode, this conforms to RFC 7692 7.1.2.2. Must"]
    #[doc = " be in range 8..15 inclusive."]
    pub server_max_window_bits: u8,
    #[doc = " The client's LZ77 sliding window size. Negotiated during the HTTP upgrade. In client mode,"]
    #[doc = " this conforms to RFC 7692 7.1.2.2. In server mode, this conforms to RFC 7692 7.1.2.2. Must"]
    #[doc = " be in range 8..15 inclusive."]
    pub client_max_window_bits: u8,
    #[doc = " Request that the server resets the LZ77 sliding window between messages - RFC 7692 7.1.1.1."]
    pub request_server_no_context_takeover: bool,
    #[doc = " Request that the server resets the LZ77 sliding window between messages - RFC 7692 7.1.1.1."]
    pub request_client_no_context_takeover: bool,
    #[doc = " Whether to accept `no_context_takeover`."]
    pub accept_no_context_takeover: bool,
    #[doc = " The active compression level."]
    #[doc = " The integer here is on a scale of 0-9 where 0 means"]
    #[doc = " \"no compression\" and 9 means \"take as long as you'd like\"."]
    pub compression_level: u8,
}
impl bytebridge::ByteCodec for ClientConfig {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, bytebridge::FromBytesError>
    where
        Self: Sized,
        R: std::io::Read,
    {
        let __repr_len: u32 = 10u32;
        if __repr_len != bytebridge::read_array_len(reader)? {
            return Err(bytebridge::FromBytesError::Invalid(format!(
                "Expected an array of len: {}",
                __repr_len
            )));
        }
        Ok(ClientConfig {
            max_message_size: <u32 as bytebridge::ByteCodec>::try_from_reader(reader).map_err(
                |e| {
                    println!("a");
                    e
                },
            )?,
            remote_buffer_size: <u32 as bytebridge::ByteCodec>::try_from_reader(reader).map_err(
                |e| {
                    println!("b");
                    e
                },
            )?,
            transport_buffer_size: <u32 as bytebridge::ByteCodec>::try_from_reader(reader)
                .map_err(|e| {
                    println!("c");
                    e
                })?,
            registration_buffer_size: <u32 as bytebridge::ByteCodec>::try_from_reader(reader)
                .map_err(|e| {
                    println!("d");
                    e
                })?,
            server_max_window_bits: <u8 as bytebridge::ByteCodec>::try_from_reader(reader)
                .map_err(|e| {
                    println!("e");
                    e
                })?,
            client_max_window_bits: <u8 as bytebridge::ByteCodec>::try_from_reader(reader)
                .map_err(|e| {
                    println!("g");
                    e
                })?,
            request_server_no_context_takeover: <bool as bytebridge::ByteCodec>::try_from_reader(
                reader,
            )
            .map_err(|e| {
                println!("h");
                e
            })?,
            request_client_no_context_takeover: <bool as bytebridge::ByteCodec>::try_from_reader(
                reader,
            )
            .map_err(|e| {
                println!("i");
                e
            })?,
            accept_no_context_takeover: <bool as bytebridge::ByteCodec>::try_from_reader(reader)
                .map_err(|e| {
                    println!("j");
                    e
                })?,
            compression_level: <u8 as bytebridge::ByteCodec>::try_from_reader(reader).map_err(
                |e| {
                    println!("k");
                    e
                },
            )?,
        })
    }
    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), std::io::Error>
    where
        W: std::io::Write,
    {
        let ClientConfig {
            max_message_size,
            remote_buffer_size,
            transport_buffer_size,
            registration_buffer_size,
            server_max_window_bits,
            client_max_window_bits,
            request_server_no_context_takeover,
            request_client_no_context_takeover,
            accept_no_context_takeover,
            compression_level,
        } = self;
        bytebridge::write_array_len(writer, 10u32)?;
        bytebridge::ByteCodec::to_bytes(max_message_size, writer)?;
        bytebridge::ByteCodec::to_bytes(remote_buffer_size, writer)?;
        bytebridge::ByteCodec::to_bytes(transport_buffer_size, writer)?;
        bytebridge::ByteCodec::to_bytes(registration_buffer_size, writer)?;
        bytebridge::ByteCodec::to_bytes(server_max_window_bits, writer)?;
        bytebridge::ByteCodec::to_bytes(client_max_window_bits, writer)?;
        bytebridge::ByteCodec::to_bytes(request_server_no_context_takeover, writer)?;
        bytebridge::ByteCodec::to_bytes(request_client_no_context_takeover, writer)?;
        bytebridge::ByteCodec::to_bytes(accept_no_context_takeover, writer)?;
        bytebridge::ByteCodec::to_bytes(compression_level, writer)?;
        Ok(())
    }
}
