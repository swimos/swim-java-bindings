package ai.swim.server.lanes.models.response;

import java.util.UUID;

public interface LaneResponseVisitor<T> {
  void visitInitialized();

  void visitEvent(T event);

  void visitSyncEvent(UUID remote, T event);

  void visitSynced(UUID remote);
}
