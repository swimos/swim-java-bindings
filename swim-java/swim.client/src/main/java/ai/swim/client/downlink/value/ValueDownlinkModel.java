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

package ai.swim.client.downlink.value;

import ai.swim.client.Handle;
import ai.swim.client.SwimClientException;
import ai.swim.client.downlink.DownlinkConfig;
import ai.swim.client.lifecycle.OnLinked;
import ai.swim.client.lifecycle.OnUnlinked;
import ai.swim.structure.Form;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public final class ValueDownlinkModel<T> extends ValueDownlink<T> {
  private ValueDownlinkModel(CountDownLatch stoppedBarrier, ValueDownlinkState<T> state) {
    super(stoppedBarrier, state);
  }

  /**
   * Opens a ValueDownlink to host/node/lane
   *
   * @param handle         a single-use SwimClient native handle that is dropped after the downlink has been opened.
   * @param host           The URl of the host to open the connection to.
   * @param node           The node URI to downlink to.
   * @param lane           The lane URI to downlink to.
   * @param formType       A form class representing the structure of the downlink's value.
   * @param lifecycle      Downlink lifecycle event callbacks.
   * @param downlinkConfig Downlink and runtime configuration.
   * @return An established ValueDownlink.
   * @throws SwimClientException if there is an error opening the downlink or by a malformed address.
   */
  @SuppressWarnings("RedundantThrows") // Thrown natively.
  static <T> ValueDownlink<T> open(Handle handle,
                                   String host,
                                   String node,
                                   String lane,
                                   Class<T> formType,
                                   ValueDownlinkLifecycle<T> lifecycle,
                                   DownlinkConfig downlinkConfig) throws SwimClientException {
    ValueDownlinkState<T> state = new ValueDownlinkState<>(Form.forClass(formType));
    CountDownLatch stoppedBarrier = new CountDownLatch(1);
    ValueDownlinkModel<T> downlink = new ValueDownlinkModel<>(stoppedBarrier, state);

    try {
      open(
          handle.get(),
          downlink,
          downlinkConfig.toArray(),
          stoppedBarrier,
          host,
          node,
          lane,
          state.wrapOnEvent(lifecycle.getOnEvent()),
          lifecycle.getOnLinked(),
          state.wrapOnSet(lifecycle.getOnSet()),
          state.wrapOnSynced(lifecycle.getOnSynced()),
          lifecycle.getOnUnlinked()
      );
    } finally {
      handle.drop();
    }

    return downlink;
  }

  /**
   * Attempts to open a new value downlink; starting a new Value Downlink Runtime as required and attaching a new native
   * value downlink to it.
   *
   * @param handlePtr      A SwimHandle pointer.
   * @param downlink       Downlink model reference for reporting any exceptions that are thrown to.
   * @param config         A byte-representation of the configuration for the downlink and the runtime.
   * @param stoppedBarrier An owned stop barrier for Java threads to block on until the downlink has terminated.
   * @param host           The URl of the host to open the connection to.
   * @param node           The node URI to downlink to.
   * @param lane           The lane URI to downlink to.
   * @param onEvent        onEvent callback. If this is null, then it will not be invoked.
   * @param onLinked       onLinked callback. If this is null, then it will not be invoked.
   * @param onSet          onSet callback. If this is null, then it will not be invoked.
   * @param onSynced       onSynced callback. If this is null, then it will not be invoked.
   * @param onUnlinked     onUnlinked callback. If this is null, then it will not be invoked.
   * @param <T>            The type of the value.
   */
  private static native <T> void open(
      long handlePtr,
      ValueDownlinkModel<T> downlink,
      byte[] config,
      CountDownLatch stoppedBarrier,
      String host,
      String node,
      String lane,
      Function<ByteBuffer, ByteBuffer> onEvent,
      OnLinked onLinked,
      Function<ByteBuffer, ByteBuffer> onSet,
      Function<ByteBuffer, ByteBuffer> onSynced,
      OnUnlinked onUnlinked
  );

}
