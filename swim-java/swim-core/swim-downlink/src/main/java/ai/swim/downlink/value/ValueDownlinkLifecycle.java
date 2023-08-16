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

package ai.swim.downlink.value;

import ai.swim.downlink.lifecycle.OnEvent;
import ai.swim.downlink.lifecycle.OnLinked;
import ai.swim.downlink.lifecycle.OnSet;
import ai.swim.downlink.lifecycle.OnSynced;
import ai.swim.downlink.lifecycle.OnUnlinked;

public class ValueDownlinkLifecycle<T> {
  private OnEvent<T> onEvent;
  private OnLinked onLinked;
  private OnSynced<T> onSynced;
  private OnSet<T> onSet;
  private OnUnlinked onUnlinked;

  public OnEvent<T> getOnEvent() {
    return onEvent;
  }

  public ValueDownlinkLifecycle<T> setOnEvent(OnEvent<T> onEvent) {
    this.onEvent = onEvent;
    return this;
  }

  public OnLinked getOnLinked() {
    return onLinked;
  }

  public ValueDownlinkLifecycle<T> setOnLinked(OnLinked onLinked) {
    this.onLinked = onLinked;
    return this;
  }

  public OnSynced<T> getOnSynced() {
    return onSynced;
  }

  public ValueDownlinkLifecycle<T> setOnSynced(OnSynced<T> onSynced) {
    this.onSynced = onSynced;
    return this;
  }

  public OnSet<T> getOnSet() {
    return onSet;
  }

  public ValueDownlinkLifecycle<T> setOnSet(OnSet<T> onSet) {
    this.onSet = onSet;
    return this;
  }

  public OnUnlinked getOnUnlinked() {
    return onUnlinked;
  }

  public ValueDownlinkLifecycle<T> setOnUnlinked(OnUnlinked onUnlinked) {
    this.onUnlinked = onUnlinked;
    return this;
  }
}
