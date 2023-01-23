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

import ai.swim.client.SwimClientException;

import java.util.concurrent.CountDownLatch;

/**
 * A Swim Value Downlink representation. A ValueDownlink synchronizes a shared real-time value with a remote value lane
 * and provides lifecycle callbacks that may be registered to be notified of certain events.
 * <p>
 * This class is thread safe.
 *
 * @param <T> the type of the value.
 */
public abstract class ValueDownlink<T> {
  private final CountDownLatch stoppedBarrier;
  /// Referenced through FFI and *possibly* set to an error that occurred while the downlink was running. Iff this is
  /// non-null *after* calling 'awaitStopped' then the downlink terminated with an error and it *must* be thrown from
  /// that method. Iff it is null then the downlink terminated successfully.
  ///
  /// This is implemented this way to avoid a long-running FFI call and so that the 'awaitStopped' method has an
  /// exception to throw when it is called - otherwise, the exception would be lost.
  protected String stoppedWith;
  // Referenced through FFI. Do not remove.
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final ValueDownlinkState<T> state;

  protected ValueDownlink(CountDownLatch stoppedBarrier, ValueDownlinkState<T> state) {
    this.stoppedBarrier = stoppedBarrier;
    this.state = state;
  }

  /**
   * Blocks the current thread until the downlink has been terminated.
   *
   * @throws SwimClientException if the downlink terminated with an error.
   */
  public void awaitStopped() throws SwimClientException {
    try {
      stoppedBarrier.await();
    } catch (InterruptedException e) {
      throw new SwimClientException(e);
    }

    if (stoppedWith != null) {
      throw new SwimClientException(stoppedWith);
    }
  }

}
