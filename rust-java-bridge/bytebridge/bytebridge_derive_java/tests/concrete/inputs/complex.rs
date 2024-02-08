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

#[bytebridge]
#[derive(Debug, Clone, PartialEq)]
pub struct Test {
    /// Timeout in seconds. If the runtime has no consumers for longer than this timeout, it will stop.
    #[bytebridge(default_value = 30)]
    pub runtime_empty_timeout: std::time::Duration,
    /// Size of the queue for accepting new subscribers to a downlink.
    #[bytebridge(natural_number, default_value = 16)]
    pub runtime_attachment_queue_size: i64,
    /// Abort the downlink on receiving invalid frames.
    #[bytebridge(default_value = true)]
    pub runtime_abort_on_bad_frames: bool,
    /// Size of the buffers to communicated with the socket.
    #[bytebridge(natural_number, default_value = 4096)]
    pub runtime_remote_buffer_size: i64,
    /// Size of the buffers to communicate with the downlink implementation.
    #[bytebridge(natural_number, default_value = 4096)]
    pub runtime_downlink_buffer_size: i64,
    /// Whether to trigger event handlers if the downlink receives events before it has
    /// synchronized.
    #[bytebridge(default_value = false)]
    pub downlink_events_when_not_synced: bool,
    /// Whether the downlink should terminate on an unlinked message.
    #[bytebridge(default_value = true)]
    pub downlink_terminate_on_unlinked: bool,
    /// Downlink event buffer capacity.
    #[bytebridge(natural_number, default_value = 1024)]
    pub downlink_buffer_size: i64,
    /// If the connection fails, it should be restarted and the consumer passed to the new
    /// connection.
    #[bytebridge(default_value = true)]
    pub keep_linked: bool,
}
