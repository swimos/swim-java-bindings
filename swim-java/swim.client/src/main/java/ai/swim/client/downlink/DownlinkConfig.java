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

  public DownlinkConfig setEmptyTimeout(int emptyTimeout) {
    nonZero(emptyTimeout);

    this.emptyTimeout = emptyTimeout;
    return this;
  }

  private void nonZero(int emptyTimeout) {
    if (emptyTimeout<=0){
      throw new IllegalArgumentException("Argument must be non-zero");
    }
  }

  public DownlinkConfig setAttachmentQueueSize(int attachmentQueueSize) {
    nonZero(emptyTimeout);

    this.attachmentQueueSize = attachmentQueueSize;
    return this;
  }

  public DownlinkConfig setAbortOnBadFrames(boolean abortOnBadFrames) {
    this.abortOnBadFrames = abortOnBadFrames;
    return this;
  }

  public DownlinkConfig setRemoteBufferSize(int remoteBufferSize) {
    nonZero(emptyTimeout);

    this.remoteBufferSize = remoteBufferSize;
    return this;
  }

  public DownlinkConfig setDownlinkBufferSize(int downlinkBufferSize) {
    nonZero(emptyTimeout);

    this.downlinkBufferSize = downlinkBufferSize;
    return this;
  }

  public DownlinkConfig setEventsWhenNotSynced(boolean eventsWhenNotSynced) {
    this.eventsWhenNotSynced = eventsWhenNotSynced;
    return this;
  }

  public DownlinkConfig setTerminateOnUnlinked(boolean terminateOnUnlinked) {
    this.terminateOnUnlinked = terminateOnUnlinked;
    return this;
  }

  public DownlinkConfig setChannelSize(int channelSize) {
    nonZero(emptyTimeout);

    this.channelSize = channelSize;
    return this;
  }

  public DownlinkConfig setKeepLinked(boolean keepLinked) {
    this.keepLinked = keepLinked;
    return this;
  }

  public DownlinkConfig setKeepSynced(boolean keepSynced) {
    this.keepSynced = keepSynced;
    return this;
  }

  public byte[] toArray() {
    ByteBuffer buffer = ByteBuffer.allocate(44);

    buffer.putLong(emptyTimeout);
    buffer.putLong(attachmentQueueSize);
    buffer.put((byte) (abortOnBadFrames ? 1 : 0));
    buffer.putLong(remoteBufferSize);
    buffer.putLong(downlinkBufferSize);
    buffer.put((byte) (eventsWhenNotSynced ? 1 : 0));
    buffer.put((byte) (terminateOnUnlinked ? 1 : 0));
    buffer.putLong(channelSize);
    buffer.put((byte) ((keepLinked ? 1 : 0) << 1 | (keepSynced ? 1 : 0)));

    return buffer.array();
  }
}
