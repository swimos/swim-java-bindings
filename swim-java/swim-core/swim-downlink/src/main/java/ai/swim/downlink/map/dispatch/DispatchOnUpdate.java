package ai.swim.downlink.map.dispatch;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface DispatchOnUpdate {
  void onUpdate(ByteBuffer keyBuffer, ByteBuffer valueBuffer, boolean dispatch);
}
