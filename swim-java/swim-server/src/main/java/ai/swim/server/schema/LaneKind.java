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

package ai.swim.server.schema;

import org.msgpack.core.MessageBufferPacker;
import java.io.IOException;

/*
 * Lane Kinds.
 * <p>
 * Note: the constants contained in this enumeration directly map to the Rust {@code LaneKindRepr} and any changes made
 * must be reflected in it. This class uses its ordinal to map the types.
 */
public enum LaneKind {
  Action,
  Command,
  Demand,
  DemandMap,
  Map,
  JoinMap,
  JoinValue,
  Supply,
  Spatial,
  Value;

  public void pack(MessageBufferPacker packer) throws IOException {
    packer.packExtensionTypeHeader((byte) 1, 1);
    packer.packInt(this.ordinal());
    packer.packArrayHeader(0);
  }
}
