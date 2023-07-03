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

import ai.swim.client.lifecycle.OnClear;
import ai.swim.client.lifecycle.OnLinked;
import ai.swim.client.lifecycle.OnRemove;
import ai.swim.client.lifecycle.OnSynced;
import ai.swim.client.lifecycle.OnUnlinked;
import ai.swim.client.lifecycle.OnUpdate;

import java.util.Map;

public class MapDownlinkLifecycle<K, V> {
  private OnUpdate<K, V> onUpdate;
  private OnLinked onLinked;
  private OnSynced<Map<K, V>> onSynced;
  private OnRemove<K, V> onRemove;
  private OnUnlinked onUnlinked;
  private OnClear<K, V> onClear;

  public OnUpdate<K, V> getOnUpdate() {
    return onUpdate;
  }

  public MapDownlinkLifecycle<K, V> setOnUpdate(OnUpdate<K, V> onUpdate) {
    this.onUpdate = onUpdate;
    return this;
  }

  public OnLinked getOnLinked() {
    return onLinked;
  }

  public MapDownlinkLifecycle<K, V> setOnLinked(OnLinked onLinked) {
    this.onLinked = onLinked;
    return this;
  }

  public OnSynced<Map<K, V>> getOnSynced() {
    return onSynced;
  }

  public MapDownlinkLifecycle<K, V> setOnSynced(OnSynced<Map<K, V>> onSynced) {
    this.onSynced = onSynced;
    return this;
  }

  public OnRemove<K, V> getOnRemove() {
    return onRemove;
  }

  public MapDownlinkLifecycle<K, V> setOnRemove(OnRemove<K, V> onRemove) {
    this.onRemove = onRemove;
    return this;
  }

  public OnUnlinked getOnUnlinked() {
    return onUnlinked;
  }

  public MapDownlinkLifecycle<K, V> setOnUnlinked(OnUnlinked onUnlinked) {
    this.onUnlinked = onUnlinked;
    return this;
  }

  public OnClear<K, V> getOnClear() {
    return onClear;
  }

  public MapDownlinkLifecycle<K, V> setOnClear(OnClear<K, V> onClear) {
    this.onClear = onClear;
    return this;
  }


}
