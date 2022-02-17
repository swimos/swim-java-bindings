package ai.swim.ffi;

import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class PointerTest {
  private static final int resourceCount = 100;
  private static final AtomicInteger nativeObjectCount = new AtomicInteger(resourceCount);

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "ResultOfMethodCallIgnored"})
  public void pack() {
    List<int[]> data = new ArrayList<>();
    for (int i = 0; i < 128; ++i) {
      data.add(new int[64]);
    }
    data.toString();
  }

  @Test
  void freesResources() {
    Deque<NativeResource> objects = new ArrayDeque<>();

    while (true) {
      pack();

      objects.add(new Marker());

      if (objects.size() > resourceCount) {
        objects.removeFirst();
      }

      if (nativeObjectCount.get() <= 0) {
        break;
      }
    }
  }

  static class Marker implements NativeResource {
    @SuppressWarnings("unused")
    private final Pointer.Destructor destructor = Pointer.newDestructor(this, nativeObjectCount::decrementAndGet);
  }
}