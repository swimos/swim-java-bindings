/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.client.downlink.value;

import ai.swim.client.Handle;
import ai.swim.client.downlink.DownlinkConfig;
import ai.swim.client.downlink.DownlinkException;
import ai.swim.client.lifecycle.OnEvent;
import ai.swim.client.lifecycle.OnLinked;
import ai.swim.client.lifecycle.OnSet;
import ai.swim.client.lifecycle.OnSynced;
import ai.swim.client.lifecycle.OnUnlinked;
import java.util.Objects;

public class ValueDownlinkBuilder<T> {
  private final Handle handle;
  private final String host;
  private final String lane;
  private final Class<T> formType;
  private final String node;
  private final ValueDownlinkLifecycle<T> lifecycle;
  private DownlinkConfig downlinkConfig;

  public ValueDownlinkBuilder(Handle handle, Class<T> formType, String host, String node, String lane) {
    this.handle = handle;
    this.formType = formType;
    this.host = host;
    this.node = node;
    this.lane = lane;
    this.lifecycle = new ValueDownlinkLifecycle<>();
    this.downlinkConfig = new DownlinkConfig();
  }

  /**
   * Registers a callback that will be invoked when the downlink receives an event.
   */
  public ValueDownlinkBuilder<T> setOnEvent(OnEvent<T> onEvent) {
    lifecycle.setOnEvent(onEvent);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink links to the remote lane.
   */
  public ValueDownlinkBuilder<T> setOnLinked(OnLinked onLinked) {
    lifecycle.setOnLinked(onLinked);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink links to the remote lane.
   */
  public ValueDownlinkBuilder<T> setOnSynced(OnSynced<T> onSynced) {
    lifecycle.setOnSynced(onSynced);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink sets a new value
   */
  public ValueDownlinkBuilder<T> setOnSet(OnSet<T> onSet) {
    lifecycle.setOnSet(onSet);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink unlinks from the remote lane.
   */
  public ValueDownlinkBuilder<T> setOnUnlinked(OnUnlinked onUnlinked) {
    lifecycle.setOnUnlinked(onUnlinked);
    return this;
  }

  /**
   * Attempts to open the downlink.
   *
   * @return an established ValueDownlink.
   * @throws DownlinkException if there was an error opening the downlink.
   */
  public ValueDownlink<T> open() throws DownlinkException {
    return ValueDownlinkModel.open(handle, host, node, lane, formType, lifecycle, downlinkConfig);
  }

  /**
   * Sets the downlink and runtime configuration.
   *
   * @throws NullPointerException if the configuration is null.
   */
  public ValueDownlinkBuilder<T> setDownlinkConfig(DownlinkConfig downlinkConfig) {
    Objects.requireNonNull(downlinkConfig);
    this.downlinkConfig = downlinkConfig;
    return this;
  }
}
