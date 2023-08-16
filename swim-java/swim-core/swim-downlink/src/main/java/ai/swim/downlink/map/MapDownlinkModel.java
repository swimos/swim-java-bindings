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

package ai.swim.downlink.map;

import ai.swim.downlink.DownlinkException;
import ai.swim.downlink.DownlinkConfig;
import ai.swim.downlink.map.dispatch.DispatchDrop;
import ai.swim.downlink.map.dispatch.DispatchOnClear;
import ai.swim.downlink.map.dispatch.DispatchOnRemove;
import ai.swim.downlink.map.dispatch.DispatchOnUpdate;
import ai.swim.downlink.map.dispatch.DispatchTake;
import ai.swim.downlink.lifecycle.OnLinked;
import ai.swim.downlink.lifecycle.OnUnlinked;
import ai.swim.concurrent.Trigger;
import ai.swim.lang.ffi.NativeHandle;
import ai.swim.structure.Form;

public final class MapDownlinkModel<K, V> extends MapDownlink<K, V> {
  private MapDownlinkModel(Trigger trigger, MapDownlinkState<K, V> state) {
    super(trigger, state);
  }

  /**
   * Opens a MapDownlink to host/node/lane
   *
   * @param handle         a single-use SwimClient native handle that is dropped after the downlink has been opened.
   * @param host           The URl of the host to open the connection to.
   * @param node           The node URI to downlink to.
   * @param lane           The lane URI to downlink to.
   * @param keyType        A form class representing the structure of the map's key type.
   * @param valueType      A form class representing the structure of the map's value type.
   * @param lifecycle      Downlink lifecycle event callbacks.
   * @param downlinkConfig Downlink and runtime configuration.
   * @return An established ValueDownlink.
   * @throws DownlinkException if there is an error opening the downlink or by a malformed address.
   */
  static <K, V> MapDownlink<K, V> open(NativeHandle handle,
      String host,
      String node,
      String lane,
      Class<K> keyType,
      Class<V> valueType,
      MapDownlinkLifecycle<K, V> lifecycle,
      DownlinkConfig downlinkConfig) throws DownlinkException {
    MapDownlinkState<K, V> state = new MapDownlinkState<>(
        Form.forClass(keyType),
        Form.forClass(valueType),
        lifecycle.getOnRemove());
    Trigger trigger = new Trigger();
    MapDownlinkModel<K, V> downlink = new MapDownlinkModel<>(trigger, state);

    try {
      open(
          handle.get(),
          downlink,
          downlinkConfig.toArray(),
          trigger,
          host,
          node,
          lane,
          lifecycle.getOnLinked(),
          state.wrapOnSynced(lifecycle.getOnSynced()),
          state.wrapOnUpdate(lifecycle.getOnUpdate()),
          state.wrapOnRemove(lifecycle.getOnRemove()),
          state.wrapOnClear(lifecycle.getOnClear()),
          lifecycle.getOnUnlinked(),
          state.take(),
          state.drop());
    } finally {
      handle.drop();
    }

    return downlink;
  }

  /**
   * Attempts to open a new map downlink; starting a new Map Downlink Runtime as required and attaching a new native
   * map downlink to it.
   *
   * @param <K>        The type of the key.
   * @param <V>        The type of the value.
   * @param handlePtr  A SwimHandle pointer.
   * @param downlink   Downlink model reference for reporting any exceptions that are thrown to.
   * @param config     A byte-representation of the configuration for the downlink and the runtime.
   * @param trigger    An owned stop barrier for Java threads to block on until the downlink has terminated.
   * @param host       The URl of the host to open the connection to.
   * @param node       The node URI to downlink to.
   * @param lane       The lane URI to downlink to.
   * @param onLinked   onLinked callback. If this is null, then it will not be invoked.
   * @param onSynced   onSynced callback. If this is null, then it will not be invoked.
   * @param onUpdate   onUpdate callback. If this is null, then it will not be invoked.
   * @param onRemove   onRemove callback. If this is null, then it will not be invoked.
   * @param onClear    onClear callback. If this is null, then it will not be invoked.
   * @param onUnlinked onUnlinked callback. If this is null, then it will not be invoked.
   * @param take       callback to invoke for a take operation.
   * @param drop       callback to invoke for a take operation.
   */
  private static native <K, V> void open(long handlePtr,
      MapDownlinkModel<K, V> downlink,
      byte[] config,
      Trigger trigger,
      String host,
      String node,
      String lane,
      OnLinked onLinked,
      Routine onSynced,
      DispatchOnUpdate onUpdate,
      DispatchOnRemove onRemove,
      DispatchOnClear onClear,
      OnUnlinked onUnlinked,
      DispatchTake take,
      DispatchDrop drop) throws DownlinkException;

}
