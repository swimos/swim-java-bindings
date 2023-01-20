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

  static <T> ValueDownlink<T> open(Handle handle,
                                   String host,
                                   String node,
                                   String lane,
                                   Class<T> formType,
                                   ValueDownlinkLifecycle<T> lifecycle
  ) {
    ValueDownlinkState<T> state = new ValueDownlinkState<>(Form.forClass(formType));
    CountDownLatch stoppedBarrier = new CountDownLatch(1);
    ValueDownlinkModel<T> downlink = new ValueDownlinkModel<>(stoppedBarrier, state);

    open(
        handle.get(),
        downlink,
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

    return downlink;
  }

  private static native <T> void open(
      long runtime,
      ValueDownlinkModel<T> downlink,
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
