#[derive(Debug, Default)]
#[bytebridge]
pub struct ClientConfig {
    #[bytebridge(default_value = 8192, range(1, 4294967295))]
    pub max_message_size: u32,
    #[bytebridge(default_value = 4096, range(1, 4294967295))]
    pub remote_buffer_size: u32,
    #[bytebridge(default_value = 32, range(1, 4294967295))]
    pub transport_buffer_size: u32,
    #[bytebridge(default_value = 32, range(1, 4294967295))]
    pub registration_buffer_size: u32,
    /// The client's LZ77 sliding window size. Negotiated during the HTTP upgrade. In client mode,
    /// this conforms to RFC 7692 7.1.2.1. In server mode, this conforms to RFC 7692 7.1.2.2. Must
    /// be in range 8..15 inclusive.
    #[cfg(feature = "deflate")]
    #[bytebridge(default_value = 15, range(8, 15))]
    pub server_max_window_bits: u8,
    /// The client's LZ77 sliding window size. Negotiated during the HTTP upgrade. In client mode,
    /// this conforms to RFC 7692 7.1.2.2. In server mode, this conforms to RFC 7692 7.1.2.2. Must
    /// be in range 8..15 inclusive.
    #[cfg(feature = "deflate")]
    #[bytebridge(default_value = 15, range(8, 15))]
    pub client_max_window_bits: u8,
    /// Request that the server resets the LZ77 sliding window between messages - RFC 7692 7.1.1.1.
    #[cfg(feature = "deflate")]
    #[bytebridge(default_value = true)]
    pub request_server_no_context_takeover: bool,
    /// Request that the server resets the LZ77 sliding window between messages - RFC 7692 7.1.1.1.
    #[cfg(feature = "deflate")]
    #[bytebridge(default_value = true)]
    pub request_client_no_context_takeover: bool,
    /// Whether to accept `no_context_takeover`.
    #[cfg(feature = "deflate")]
    #[bytebridge(default_value = true)]
    pub accept_no_context_takeover: bool,
    /// The active compression level. The integer here is typically on a scale of 0-9 where 0 means
    /// "no compression" and 9 means "take as long as you'd like".
    #[cfg(feature = "deflate")]
    #[bytebridge(default_value = 1, range(0, 9))]
    pub compression_level: u8,
}
