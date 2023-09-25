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

package ai.swim.client.downlink.map;

import ai.swim.client.Handle;
import ai.swim.client.SwimClientException;
import ai.swim.client.downlink.DownlinkConfig;
import ai.swim.client.lifecycle.OnClear;
import ai.swim.client.lifecycle.OnLinked;
import ai.swim.client.lifecycle.OnRemove;
import ai.swim.client.lifecycle.OnSynced;
import ai.swim.client.lifecycle.OnUnlinked;
import ai.swim.client.lifecycle.OnUpdate;
import java.util.Map;
import java.util.Objects;

public class MapDownlinkBuilder<K, V> {
  private final Handle handle;
  private final String host;
  private final String lane;
  private final Class<K> keyType;
  private final Class<V> valueType;
  private final String node;
  private final MapDownlinkLifecycle<K, V> lifecycle;
  private DownlinkConfig downlinkConfig;

  public MapDownlinkBuilder(Handle handle,
      Class<K> keyType,
      Class<V> valueType,
      String host,
      String node,
      String lane) {
    this.handle = handle;
    this.keyType = keyType;
    this.valueType = valueType;
    this.host = host;
    this.node = node;
    this.lane = lane;
    this.lifecycle = new MapDownlinkLifecycle<>();
    this.downlinkConfig = new DownlinkConfig();
  }

  /**
   * Registers a callback that will be invoked when the downlink links to the remote lane.
   */
  public MapDownlinkBuilder<K, V> setOnLinked(OnLinked onLinked) {
    lifecycle.setOnLinked(onLinked);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink links to the remote lane.
   */
  public MapDownlinkBuilder<K, V> setOnSynced(OnSynced<Map<K, V>> onSynced) {
    lifecycle.setOnSynced(onSynced);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink unlinks from the remote lane.
   */
  public MapDownlinkBuilder<K, V> setOnUnlinked(OnUnlinked onUnlinked) {
    lifecycle.setOnUnlinked(onUnlinked);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink updates a key.
   */
  public MapDownlinkBuilder<K, V> setOnUpdate(OnUpdate<K, V> onUpdate) {
    lifecycle.setOnUpdate(onUpdate);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink removes a key.
   */
  public MapDownlinkBuilder<K, V> setOnRemove(OnRemove<K, V> onRemove) {
    lifecycle.setOnRemove(onRemove);
    return this;
  }

  /**
   * Registers a callback that will be invoked when the downlink clears its state.
   */
  public MapDownlinkBuilder<K, V> setOnClear(OnClear<K, V> onClear) {
    lifecycle.setOnClear(onClear);
    return this;
  }

  /**
   * Attempts to open the downlink.
   *
   * @return an established ValueDownlink.
   * @throws SwimClientException if there was an error opening the downlink.
   */
  public MapDownlink<K, V> open() throws SwimClientException {
    return MapDownlinkModel.open(handle, host, node, lane, keyType, valueType, lifecycle, downlinkConfig);
  }

  /**
   * Sets the downlink and runtime configuration.
   *
   * @throws NullPointerException if the configuration is null.
   */
  public MapDownlinkBuilder<K, V> setDownlinkConfig(DownlinkConfig downlinkConfig) {
    Objects.requireNonNull(downlinkConfig);
    this.downlinkConfig = downlinkConfig;
    return this;
  }
}
