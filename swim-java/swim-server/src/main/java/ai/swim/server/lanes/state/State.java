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

package ai.swim.server.lanes.state;

import ai.swim.codec.data.ByteWriter;
import ai.swim.server.lanes.WriteResult;
import java.util.UUID;

/**
 * State definition interface for operating with a {@link StateCollector}.
 */
public interface State {
  /**
   * Write the current state of the lane into {@code bytes}.
   *
   * @param bytes to write into.
   * @return a result stating whether there is more data to write.
   */
  WriteResult writeInto(ByteWriter bytes);
}
