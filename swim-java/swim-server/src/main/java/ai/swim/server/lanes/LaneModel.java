package ai.swim.server.lanes;

import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.DecoderException;
import java.util.UUID;

public abstract class LaneModel {
  public abstract void dispatch(ReadBuffer buffer) throws DecoderException;

  public abstract void sync(UUID remote) throws DecoderException;

  public abstract void init(ReadBuffer buffer) throws DecoderException;

  public abstract LaneView getLaneView();
}
