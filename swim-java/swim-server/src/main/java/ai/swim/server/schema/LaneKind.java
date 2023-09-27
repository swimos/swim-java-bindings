package ai.swim.server.schema;

import org.msgpack.core.MessageBufferPacker;
import java.io.IOException;

/**
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
