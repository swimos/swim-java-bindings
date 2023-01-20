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

import ai.swim.client.lifecycle.OnEvent;
import ai.swim.client.lifecycle.OnLinked;
import ai.swim.client.lifecycle.OnSet;
import ai.swim.client.lifecycle.OnSynced;
import ai.swim.client.lifecycle.OnUnlinked;

public abstract class ValueDownlinkBuilderModel<T> {
  protected final String host;
  protected final String lane;
  protected final Class<T> formType;
  protected final String node;
  protected final ValueDownlinkLifecycle<T> lifecycle;

  protected ValueDownlinkBuilderModel(Class<T> formType, String host, String node, String lane) {
    this.formType = formType;
    this.host = host;
    this.node = node;
    this.lane = lane;
    this.lifecycle = new ValueDownlinkLifecycle<>();
  }

  public ValueDownlinkBuilderModel<T> setOnEvent(OnEvent<T> onEvent) {
    lifecycle.setOnEvent(onEvent);
    return this;
  }

  public ValueDownlinkBuilderModel<T> setOnLinked(OnLinked onLinked) {
    lifecycle.setOnLinked(onLinked);
    return this;
  }

  public ValueDownlinkBuilderModel<T> setOnSynced(OnSynced<T> onSynced) {
    lifecycle.setOnSynced(onSynced);
    return this;
  }

  public ValueDownlinkBuilderModel<T> setOnSet(OnSet<T> onSet) {
    lifecycle.setOnSet(onSet);
    return this;
  }

  public ValueDownlinkBuilderModel<T> setOnUnlinked(OnUnlinked onUnlinked) {
    lifecycle.setOnUnlinked(onUnlinked);
    return this;
  }

  public abstract ValueDownlink<T> open();

}
