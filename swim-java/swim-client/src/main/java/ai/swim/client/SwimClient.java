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

package ai.swim.client;

import ai.swim.client.downlink.value.ValueDownlinkBuilder;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import java.io.IOException;
import java.util.Arrays;

/**
 * A SwimClient class used for opening downlinks.
 * <p>
 * This class is **not** thread safe. If shared access is required then synchronization must be performed, or it must be
 * placed behind a lock.
 */
public class SwimClient {
  static {
    System.loadLibrary("swim_client");
  }

  /**
   * A pointer to the native SwimClient instance.
   */
  private final long runtime;
  /**
   * Flag indicating whether this SwimClient has already initiated a shutdown. Used to prevent a double free.
   */
  private boolean running;

  private SwimClient(long ptr) {
    this.runtime = ptr;
    this.running = true;
  }

  /**
   * Starts the client runtime using the provided configuration and returns an established client.
   */
  public static SwimClient open(ClientConfig config) throws IOException {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      config.pack(packer);
      byte[] bytes = packer.toByteArray();
      System.out.println(Arrays.toString(bytes));
      return new SwimClient(startClient(packer.toByteArray()));
    }
  }

  /**
   * Starts the client runtime using the default configuration and returns an established client.
   */
  public static SwimClient open() throws IOException {
    return open(new ClientConfig());
  }

  private static native long startClient(byte[] config);

  private static native long shutdownClient(long runtime);

  /**
   * Signals to the runtime that it should initiate a shutdown.
   */
  public void close() {
    if (!running) {
      throw new IllegalStateException("Already closed");
    } else {
      shutdownClient(runtime);
      running = false;
    }
  }

  /**
   * Creates a new value downlink builder.
   *
   * @param host     The URl of the host to open the connection to.
   * @param node     The node URI to downlink to.
   * @param lane     The lane URI to downlink to.
   * @param formType A form class representing the structure of the downlink's value.
   * @param <T>      The type of the downlink's value.
   * @return A value downlink builder.
   */
  public <T> ValueDownlinkBuilder<T> valueDownlink(String host, String node, String lane, Class<T> formType) {
    return new ValueDownlinkBuilder<>(Handle.create(runtime), formType, host, node, lane);
  }
}
