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

package ai.swim.server.agent.lanes.map;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.server.agent.call.CallContext;
import ai.swim.server.lanes.map.MapLaneModel;
import ai.swim.server.lanes.map.MapLaneView;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import ai.swim.structure.writer.std.ScalarWriters;
import org.junit.jupiter.api.Test;
import java.util.List;

class MapLaneModelTest {

  private static MapLaneModel<Integer, Integer> init(StateCollector collector) {
    return new MapLaneModel<>(
        0,
        new MapLaneView<>(Form.forClass(Integer.class), Form.forClass(Integer.class)),
        collector);
  }

  private static ByteWriter encodeEvents(List<MapOperation<Integer, Integer>> events) {
    ByteWriter buffer = new ByteWriter(128);
    MapOperationEncoder<Integer, Integer> encoder = new MapOperationEncoder<>(
        ScalarWriters.INTEGER,
        ScalarWriters.INTEGER);

    for (MapOperation<Integer, Integer> op : events) {
      encoder.encode(op, buffer);
    }

    return buffer;
  }

  @Test
  public void test() throws DecoderException {
    StateCollector collector = new StateCollector();
    MapLaneModel<Integer, Integer> lane = init(collector);

    ByteWriter buffer = encodeEvents(List.of(
        MapOperation.update(1, 1),
        MapOperation.update(2, 2),
        MapOperation.update(3, 3),
        MapOperation.remove(2),
        MapOperation.clear()));

    CallContext.enter();

    lane.dispatch(buffer.reader());
  }
}