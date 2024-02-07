package ai.swim.codec.encoder;

import ai.swim.codec.data.ByteWriter;
import java.util.UUID;

public class UUIDEncoder implements Encoder<UUID> {
  @Override
  public void encode(UUID target, ByteWriter buffer) {
    buffer.writeLong(target.getMostSignificantBits());
    buffer.writeLong(target.getLeastSignificantBits());
  }
}
