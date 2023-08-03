package ai.swim.server.schema;

import org.msgpack.core.MessageBufferPacker;
import java.io.IOException;

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
