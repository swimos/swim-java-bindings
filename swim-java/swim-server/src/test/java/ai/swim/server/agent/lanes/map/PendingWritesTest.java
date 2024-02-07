package ai.swim.server.agent.lanes.map;

import ai.swim.codec.data.ByteReader;
import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.map.MapLaneState;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.server.lanes.map.PendingWrites;
import ai.swim.server.lanes.map.TypedHashMap;
import ai.swim.server.lanes.map.TypedMap;
import ai.swim.server.lanes.map.codec.MapOperationDecoder;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.agent.lanes.models.response.IdentifiedLaneResponseDecoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.models.response.LaneResponseDecoder;
import ai.swim.server.lanes.models.response.LaneResponseVisitor;
import ai.swim.structure.Form;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PendingWritesTest {

  private static <T> LaneResponseVisitor<T> expectSyncEvent(UUID expectedRemote, T expectedValue) {
    Runnable failFn = () -> fail(String.format("Expected sync event %s -> %s", expectedRemote, expectedValue));

    return new LaneResponseVisitor<T>() {
      @Override
      public void visitInitialized() {
        failFn.run();
      }

      @Override
      public void visitEvent(T event) {
        failFn.run();
      }

      @Override
      public void visitSyncEvent(UUID remote, T event) {
        assertEquals(expectedRemote, remote);
        assertEquals(expectedValue, event);
      }

      @Override
      public void visitSynced(UUID remote) {
        failFn.run();
      }
    };
  }

  private static <T> LaneResponseVisitor<T> expectEvent(T expectedValue) {
    Runnable failFn = () -> fail(String.format("Expected event %s", expectedValue));

    return new LaneResponseVisitor<T>() {
      @Override
      public void visitInitialized() {
        failFn.run();
      }

      @Override
      public void visitEvent(T event) {
        assertEquals(expectedValue, event);
      }

      @Override
      public void visitSyncEvent(UUID remote, T event) {
        failFn.run();
      }

      @Override
      public void visitSynced(UUID remote) {
        failFn.run();
      }
    };
  }

  private static <T> LaneResponseVisitor<T> expectSynced(UUID expectedRemote) {
    Runnable failFn = () -> fail(String.format("Expected synced %s", expectedRemote));

    return new LaneResponseVisitor<T>() {
      @Override
      public void visitInitialized() {
        failFn.run();
      }

      @Override
      public void visitEvent(T event) {
        failFn.run();
      }

      @Override
      public void visitSyncEvent(UUID remote, T event) {
        failFn.run();
      }

      @Override
      public void visitSynced(UUID remote) {
        assertEquals(expectedRemote, remote);
      }
    };
  }

  private static <T> Decoder<IdentifiedLaneResponse<T>> decodeAndVisit(Decoder<IdentifiedLaneResponse<T>> decoder,
      ByteReader buffer,
      Supplier<LaneResponseVisitor<T>> visitor) throws DecoderException {

    decoder = decoder.decode(buffer);
    assertTrue(decoder.isDone());

    IdentifiedLaneResponse<T> laneResponse = decoder.bind();
    assertEquals(0, laneResponse.getLaneId());
    LaneResponse<T> response = laneResponse.getLaneResponse();
    response.accept(visitor.get());

    return decoder.reset();
  }

  @Test
  void writesInterleaved() throws DecoderException {
    PendingWrites<Integer, Integer> pendingWrites = new PendingWrites<>();
    Form<Integer> integerForm = Form.forClass(Integer.class);
    TypedHashMap<Integer, Integer> state = TypedMap.of(Map.of(1, 1, 2, 2, 3, 3));
    UUID firstRemote = UUID.randomUUID();
    UUID secondRemote = UUID.randomUUID();

    pendingWrites.pushSync(firstRemote, new HashSet<>(state.keySet()));
    pendingWrites.pushSync(secondRemote, new HashSet<>(state.keySet()));
    pendingWrites.pushOperation(MapOperation.update(1, 1));
    pendingWrites.pushOperation(MapOperation.remove(6));

    ByteWriter buffer = new ByteWriter();
    WriteResult writeResult = pendingWrites.writeInto(0, state, buffer, integerForm, integerForm);

    assertEquals(WriteResult.Done, writeResult);
    assertEquals(3, state.keySet().size());

    Decoder<IdentifiedLaneResponse<MapOperation<Integer, Integer>>> decoder = new IdentifiedLaneResponseDecoder<>(new LaneResponseDecoder<>(
        new MapOperationDecoder<>(integerForm, integerForm)));
    ByteReader reader = buffer.reader();

    // The first sync request will produce events that are interleaved with the operations
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(firstRemote, MapOperation.update(1, 1)));
    decoder = decodeAndVisit(decoder, reader, () -> expectEvent(MapOperation.update(1, 1)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(firstRemote, MapOperation.update(2, 2)));
    decoder = decodeAndVisit(decoder, reader, () -> expectEvent(MapOperation.remove(6)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(firstRemote, MapOperation.update(3, 3)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSynced(firstRemote));

    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(secondRemote, MapOperation.update(1, 1)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(secondRemote, MapOperation.update(2, 2)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(secondRemote, MapOperation.update(3, 3)));
    decodeAndVisit(decoder, reader, () -> expectSynced(secondRemote));

    assertTrue(reader.isEmpty());
    writeResult = pendingWrites.writeInto(0, state, buffer, integerForm, integerForm);
    assertEquals(WriteResult.NoData, writeResult);
  }

  @Test
  void syncOnly() throws DecoderException {
    PendingWrites<Integer, Integer> pendingWrites = new PendingWrites<>();
    Form<Integer> integerForm = Form.forClass(Integer.class);
    TypedHashMap<Integer, Integer> state = TypedMap.of(Map.of(1, 1, 2, 2, 3, 3));
    UUID firstRemote = UUID.randomUUID();
    UUID secondRemote = UUID.randomUUID();

    pendingWrites.pushSync(firstRemote, new HashSet<>(state.keySet()));
    pendingWrites.pushSync(secondRemote, new HashSet<>(state.keySet()));

    ByteWriter buffer = new ByteWriter();
    WriteResult writeResult = pendingWrites.writeInto(0, state, buffer, integerForm, integerForm);

    assertEquals(WriteResult.Done, writeResult);
    assertEquals(3, state.keySet().size());

    Decoder<IdentifiedLaneResponse<MapOperation<Integer, Integer>>> decoder = new IdentifiedLaneResponseDecoder<>(new LaneResponseDecoder<>(
        new MapOperationDecoder<>(integerForm, integerForm)));
    ByteReader reader = buffer.reader();

    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(firstRemote, MapOperation.update(1, 1)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(firstRemote, MapOperation.update(2, 2)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(firstRemote, MapOperation.update(3, 3)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSynced(firstRemote));

    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(secondRemote, MapOperation.update(1, 1)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(secondRemote, MapOperation.update(2, 2)));
    decoder = decodeAndVisit(decoder, reader, () -> expectSyncEvent(secondRemote, MapOperation.update(3, 3)));
    decodeAndVisit(decoder, reader, () -> expectSynced(secondRemote));

    assertTrue(reader.isEmpty());

    writeResult = pendingWrites.writeInto(0, state, buffer, integerForm, integerForm);
    assertEquals(WriteResult.NoData, writeResult);
  }

  @Test
  void eventsOnly() throws DecoderException {
    PendingWrites<Integer, Integer> pendingWrites = new PendingWrites<>();
    Form<Integer> integerForm = Form.forClass(Integer.class);
    TypedHashMap<Integer, Integer> state = TypedMap.of(Map.of(1, 1, 2, 2, 3, 3));

    pendingWrites.pushOperation(MapOperation.update(1, 1));
    pendingWrites.pushOperation(MapOperation.remove(6));
    pendingWrites.pushOperation(MapOperation.clear());

    ByteWriter buffer = new ByteWriter();
    WriteResult writeResult = pendingWrites.writeInto(0, state, buffer, integerForm, integerForm);

    assertEquals(WriteResult.Done, writeResult);
    assertEquals(3, state.keySet().size());

    Decoder<IdentifiedLaneResponse<MapOperation<Integer, Integer>>> decoder = new IdentifiedLaneResponseDecoder<>(new LaneResponseDecoder<>(
        new MapOperationDecoder<>(integerForm, integerForm)));
    ByteReader reader = buffer.reader();

    decoder = decodeAndVisit(decoder, reader, () -> expectEvent(MapOperation.update(1, 1)));
    decoder = decodeAndVisit(decoder, reader, () -> expectEvent(MapOperation.remove(6)));
    decodeAndVisit(decoder, reader, () -> expectEvent(MapOperation.clear()));

    assertTrue(reader.isEmpty());

    writeResult = pendingWrites.writeInto(0, state, buffer, integerForm, integerForm);
    assertEquals(WriteResult.NoData, writeResult);
  }

}