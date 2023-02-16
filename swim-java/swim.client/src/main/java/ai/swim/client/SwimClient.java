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

import ai.swim.client.downlink.map.MapDownlinkBuilder;
import ai.swim.client.downlink.value.ValueDownlinkBuilder;
import ai.swim.lang.ffi.NativeResource;
import ai.swim.lang.ffi.Pointer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A SwimClient class used for opening downlinks.
 * <p>
 * This class is **not** thread safe. If shared access is required then synchronization must be performed, or it must be
 * placed behind a lock.
 */
public class SwimClient implements NativeResource {
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
  private final AtomicBoolean running;

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;

  private SwimClient(long ptr) {
    AtomicBoolean running = new AtomicBoolean(true);
    this.runtime = ptr;
    this.running = running;
    this.destructor = Pointer.newDestructor(this, () -> {
      if (running.get()) {
        SwimClient.shutdownClient(ptr);
      }
    });
  }

  /**
   * Starts the client runtime and returns an established client.
   */
  public static SwimClient open() {
    return new SwimClient(startClient());
  }

  private static native long startClient();

  private static native long shutdownClient(long runtime);

  /**
   * Signals to the runtime that it should initiate a shutdown.
   */
  public void close() {
    if (!running.get()) {
      throw new IllegalStateException("Already closed");
    } else {
      shutdownClient(runtime);
      running.set(false);
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

  /**
   * Creates a new map downlink builder.
   *
   * @param host      The URl of the host to open the connection to.
   * @param node      The node URI to downlink to.
   * @param lane      The lane URI to downlink to.
   * @param keyForm   A form class representing the structure of the downlink's key.
   * @param valueForm A form class representing the structure of the downlink's value.
   * @param <K>       The type of the downlink's key.
   * @param <V>       The type of the downlink's value.
   * @return A map downlink builder.
   */
  public <K, V> MapDownlinkBuilder<K, V> mapDownlink(String host, String node, String lane, Class<K> keyForm, Class<V> valueForm) {
    return new MapDownlinkBuilder<>(Handle.create(runtime), keyForm, valueForm, host, node, lane);
  }
}
