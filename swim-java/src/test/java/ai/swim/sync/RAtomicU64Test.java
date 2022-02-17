package ai.swim.sync;

import ai.swim.JniRunner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class RAtomicU64Test extends JniRunner {

  @Test
  void testAtomicU64Load() {
    RAtomicU64 atomicU64 = RAtomicU64.create(64);
    assertEquals(atomicU64.load(Ordering.Relaxed), 64);
  }

  @Test
  void testAtomicU64Store() {
    RAtomicU64 atomicU64 = RAtomicU64.create(8);
    assertEquals(atomicU64.load(Ordering.Relaxed), 8);

    atomicU64.store(16, Ordering.Relaxed);
    assertEquals(atomicU64.load(Ordering.Relaxed), 16);
  }

  @Test
  void testIllegalArgumentCreate() {
    try {
      RAtomicU64.create(-8);
    } catch (IllegalArgumentException e) {
      return;
    }

    fail("Expected an illegal argument exception to be thrown when creating an RAtomicU64 with a negative number");
  }

  @Test
  void testIllegalArgumentStore() {
    RAtomicU64 atomicU64 = RAtomicU64.create(8);
    assertEquals(atomicU64.load(Ordering.Relaxed), 8);

    try {
      atomicU64.store(-8, Ordering.Relaxed);
    } catch (IllegalArgumentException e) {
      return;
    }

    fail("Expected an illegal argument exception to be thrown when storing a negative value in an RAtomicU64");
  }
}
