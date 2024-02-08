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

package ai.swim.server.agent.lanes.map.codec;

import ai.swim.codec.data.ByteReader;
import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.server.lanes.map.codec.MapOperationDecoder;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import ai.swim.structure.writer.std.ScalarWriters;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MapOperationDecoderTest {

  void roundTrip(MapOperation<Integer, Integer> mapOperation) throws DecoderException {
    MapOperationEncoder<Integer, Integer> encoder = new MapOperationEncoder<>(
        ScalarWriters.INTEGER,
        ScalarWriters.INTEGER);

    ByteWriter writer = new ByteWriter();
    encoder.encode(mapOperation, writer);

    ByteReader reader = writer.reader();

    Decoder<MapOperation<Integer, Integer>> decoder = new MapOperationDecoder<>(
        ScalarRecognizer.INTEGER,
        ScalarRecognizer.INTEGER);
    decoder = decoder.decode(reader);

    assertTrue(decoder.isDone());
    assertEquals(mapOperation, decoder.bind());
  }

  @Test
  public void roundTripRemove() throws DecoderException {
    roundTrip(MapOperation.remove(1));
  }

  @Test
  public void roundTripClear() throws DecoderException {
    roundTrip(MapOperation.clear());
  }

  @Test
  public void roundTripUpdate() throws DecoderException {
    roundTrip(MapOperation.update(1, 2));
  }

  @Test
  public void multipleMessages() throws DecoderException {
    MapOperationEncoder<Integer, Integer> encoder = new MapOperationEncoder<>(
        ScalarWriters.INTEGER,
        ScalarWriters.INTEGER);
    ByteWriter writer = new ByteWriter();

    List<MapOperation<Integer, Integer>> expected = new ArrayList<>(List.of(
        MapOperation.update(1, 2),
        MapOperation.update(3, 4),
        MapOperation.clear(),
        MapOperation.remove(5),
        MapOperation.update(6, 7)));

    for (MapOperation<Integer, Integer> op : expected) {
      encoder.encode(op, writer);
    }

    Decoder<MapOperation<Integer, Integer>> decoder = new MapOperationDecoder<>(
        ScalarRecognizer.INTEGER,
        ScalarRecognizer.INTEGER);
    ByteReader reader = writer.reader();
    ListIterator<MapOperation<Integer, Integer>> iterator = expected.listIterator();

    while (iterator.hasNext()) {
      MapOperation<Integer, Integer> op = iterator.next();
      decoder = decoder.decode(reader);

      if (decoder.isDone()) {
        MapOperation<Integer, Integer> actual = decoder.bind();
        assertEquals(op, actual);
        iterator.remove();
        decoder = decoder.reset();
      } else {
        fail("Expected decoder to be done: " + decoder);
      }
    }

    assertTrue(expected.isEmpty());
  }

}