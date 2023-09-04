package ai.swim.server.lanes.map.codec;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.structure.writer.Writable;

public class MapOperationEncoder<K, V> implements Encoder<MapOperation<K, V>> {
  private final Writable<K> kWriter;
  private final Writable<V> vWriter;

  public MapOperationEncoder(Writable<K> kWriter, Writable<V> vWriter) {
    this.kWriter = kWriter;
    this.vWriter = vWriter;
  }

  @Override
  public void encode(MapOperation<K, V> target, ByteWriter buffer) {
    target.encode(kWriter, vWriter, buffer);
  }

}
