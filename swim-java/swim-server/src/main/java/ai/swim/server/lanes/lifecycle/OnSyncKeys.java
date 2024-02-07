package ai.swim.server.lanes.lifecycle;

import java.util.Iterator;

@FunctionalInterface
public interface OnSyncKeys<K> {
  Iterator<K> onSyncKeys();
}
