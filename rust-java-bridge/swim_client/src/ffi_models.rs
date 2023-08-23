#[derive(Debug, Default)]
#[bytebridge]
pub struct ClientConfig {
    #[bytebridge(default_value = 8192, natural_number)]
    pub max_message_size: u32,
    #[bytebridge(default_value = 4096, natural_number)]
    pub remote_buffer_size: u32,
    #[bytebridge(default_value = 32, natural_number)]
    pub transport_buffer_size: u32,
    #[bytebridge(default_value = 32, natural_number)]
    pub registration_buffer_size: u32,
    /// The client's LZ77 sliding window size. Negotiated during the HTTP upgrade. In client mode,
    /// this conforms to RFC 7692 7.1.2.1. In server mode, this conforms to RFC 7692 7.1.2.2. Must
    /// be in range 8..15 inclusive.
    #[bytebridge(default_value = 15, range(8, 15))]
    pub server_max_window_bits: u8,
    /// The client's LZ77 sliding window size. Negotiated during the HTTP upgrade. In client mode,
    /// this conforms to RFC 7692 7.1.2.2. In server mode, this conforms to RFC 7692 7.1.2.2. Must
    /// be in range 8..15 inclusive.
    #[bytebridge(default_value = 15, range(8, 15))]
    pub client_max_window_bits: u8,
    /// Request that the server resets the LZ77 sliding window between messages - RFC 7692 7.1.1.1.
    #[bytebridge(default_value = true)]
    pub request_server_no_context_takeover: bool,
    /// Request that the server resets the LZ77 sliding window between messages - RFC 7692 7.1.1.1.
    #[bytebridge(default_value = true)]
    pub request_client_no_context_takeover: bool,
    /// Whether to accept `no_context_takeover`.
    #[bytebridge(default_value = true)]
    pub accept_no_context_takeover: bool,
    /// The active compression level.
    /// The integer here is on a scale of 0-9 where 0 means
    /// "no compression" and 9 means "take as long as you'd like".
    #[bytebridge(default_value = 1, range(0, 9))]
    pub compression_level: u8,
}
