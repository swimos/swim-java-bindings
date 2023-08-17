// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.client.downlink;

import java.nio.ByteBuffer;

/**
 * Swim downlink runtime configuration, downlink configuration and downlink link options.
 */
public class DownlinkConfig {
  private int emptyTimeout = 30;
  private int attachmentQueueSize = 16;
  private boolean abortOnBadFrames = true;
  private int remoteBufferSize = 4096;
  private int downlinkBufferSize = 1024;
  private boolean eventsWhenNotSynced = false;
  private boolean terminateOnUnlinked = true;
  private int channelSize = 1024;
  private boolean keepLinked = true;
  private boolean keepSynced = true;

  /**
   * Sets the duration that the runtime may be inactive for before it shuts down; a runtime is considered inactive if
   * it has no consumers.
   *
   * @param emptyTimeout the timeout duration in seconds.
   * @throws IllegalArgumentException it the timeout is less than 1 second.
   */
  public DownlinkConfig setEmptyTimeout(int emptyTimeout) {
    nonZero(emptyTimeout);

    this.emptyTimeout = emptyTimeout;
    return this;
  }

  private void nonZero(int emptyTimeout) {
    if (emptyTimeout <= 0) {
      throw new IllegalArgumentException("Argument must be non-zero");
    }
  }

  /**
   * Sets the queue size for attaching new subscribers to the runtime.
   *
   * @param attachmentQueueSize the queue size.
   * @throws IllegalArgumentException if the queue size is less than 1.
   */
  public DownlinkConfig setAttachmentQueueSize(int attachmentQueueSize) {
    nonZero(emptyTimeout);

    this.attachmentQueueSize = attachmentQueueSize;
    return this;
  }

  /**
   * Sets whether the runtime should shut down if it receives an invalid WARP frame.
   */
  public DownlinkConfig setAbortOnBadFrames(boolean abortOnBadFrames) {
    this.abortOnBadFrames = abortOnBadFrames;
    return this;
  }

  /**
   * Sets the size of the buffer for communicating with the socket.
   *
   * @param remoteBufferSize the size of the buffer to the socket.
   * @throws IllegalArgumentException if the buffer size is less than 1.
   */
  public DownlinkConfig setRemoteBufferSize(int remoteBufferSize) {
    nonZero(emptyTimeout);

    this.remoteBufferSize = remoteBufferSize;
    return this;
  }

  /**
   * Sets the size of the buffer between the runtime and the downlink implementation
   *
   * @param downlinkBufferSize the size of the buffer.
   * @throws IllegalArgumentException if the buffer size is less than 1.
   */
  public DownlinkConfig setDownlinkBufferSize(int downlinkBufferSize) {
    nonZero(emptyTimeout);

    this.downlinkBufferSize = downlinkBufferSize;
    return this;
  }

  /**
   * Sets whether the downlink should invoke event callbacks before it has synced.
   *
   * @param eventsWhenNotSynced whether to invoke the event callbacks before it has synced.
   */
  public DownlinkConfig setEventsWhenNotSynced(boolean eventsWhenNotSynced) {
    this.eventsWhenNotSynced = eventsWhenNotSynced;
    return this;
  }

  /**
   * Sets whether the downlink should terminate on an unlinked message.
   *
   * @param terminateOnUnlinked whether to terminate on an unlinked message.
   */
  public DownlinkConfig setTerminateOnUnlinked(boolean terminateOnUnlinked) {
    this.terminateOnUnlinked = terminateOnUnlinked;
    return this;
  }

  /**
   * @return
   */
  public int getAttachmentQueueSize() {
    return attachmentQueueSize;
  }

  /**
   * Sets whether the downlink should attempt to keep linked.
   */
  public DownlinkConfig setKeepLinked(boolean keepLinked) {
    this.keepLinked = keepLinked;
    return this;
  }

  /**
   * Sets whether the downlink should be synced.
   */
  public DownlinkConfig setKeepSynced(boolean keepSynced) {
    this.keepSynced = keepSynced;
    return this;
  }

  private static byte booleanToByte(boolean b) {
    return (byte) (b ? 1 : 0);
  }

  /**
   * Returns a byte array representation of the current configuration.
   */
  public byte[] toArray() {
    ByteBuffer buffer = ByteBuffer.allocate(44);
    buffer.putLong(emptyTimeout);
    buffer.putLong(attachmentQueueSize);
    buffer.put(booleanToByte(abortOnBadFrames));
    buffer.putLong(remoteBufferSize);
    buffer.putLong(downlinkBufferSize);
    buffer.put(booleanToByte(eventsWhenNotSynced));
    buffer.put(booleanToByte(terminateOnUnlinked));
    buffer.putLong(channelSize);
    buffer.put((byte) ((keepLinked ? 1 : 0) << 1 | (keepSynced ? 1 : 0)));

    return buffer.array();
  }
}
