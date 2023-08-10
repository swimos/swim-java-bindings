package ai.swim.client.downlink.map.dispatch;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface DispatchOnRemove {
  void onRemove(ByteBuffer keyBuffer, boolean dispatch);
}
