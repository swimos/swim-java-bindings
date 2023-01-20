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

public class SwimClient {
  static {
    System.loadLibrary("swim_client");
  }

  private final long runtime;
  private boolean running;

  private SwimClient(long ptr) {
    this.runtime = ptr;
    this.running = true;
  }

  public static SwimClient open() {
    return new SwimClient(startClient());
  }

  private static native long handle(long ptr);

  private static native long startClient();

  private static native long shutdownClient(long runtime);

  public void close() {
    if (!running) {
      throw new IllegalStateException("Already closed");
    } else {
      shutdownClient(runtime);
      running = false;
    }
  }

  public <T> ValueDownlinkBuilder<T> valueDownlink(String host, String node, String lane, Class<T> formType) {
    return new ValueDownlinkBuilder<>(handle(), formType, host, node, lane);
  }

  private Handle handle() {
    return new Handle(handle(runtime));
  }
}
