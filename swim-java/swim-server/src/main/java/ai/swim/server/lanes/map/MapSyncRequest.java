package ai.swim.server.lanes.map;

import ai.swim.codec.data.BufferOverflowException;
import ai.swim.codec.data.ByteWriter;
import ai.swim.server.lanes.MapLookup;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponseEncoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.structure.writer.Writable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MapSyncRequest<K> {
  private final UUID remote;
  private final Iterator<K> keyIter;

  public MapSyncRequest(UUID remote, Iterator<K> keyIter) {
    this.remote = remote;
    this.keyIter = keyIter;
  }

  public <V> boolean encodeInto(int laneId,
      ByteWriter byteWriter,
      Writable<K> keyForm,
      Writable<V> valueForm,
      MapLookup<K, V> mapLookup) throws BufferOverflowException {
    IdentifiedLaneResponseEncoder<MapOperation<K, V>> encoder = new IdentifiedLaneResponseEncoder<>(new MapOperationEncoder<>(
        keyForm,
        valueForm));

    if (keyIter.hasNext()) {
      while (keyIter.hasNext()) {
        K key = keyIter.next();
        V value = mapLookup.get(key);

        if (value != null) {
          encoder.encode(
              new IdentifiedLaneResponse<>(laneId, LaneResponse.syncEvent(remote, MapOperation.update(key, value))),
              byteWriter);
          return false;
        }
      }
    } else {
      encoder.encode(new IdentifiedLaneResponse<>(laneId, LaneResponse.synced(remote)), byteWriter);
    }
    return true;
  }

  @Override
  public String toString() {
    return "MapSyncRequest{" +
        "remote=" + remote +
        ", keyIter=" + keyIter +
        '}';
  }
}
