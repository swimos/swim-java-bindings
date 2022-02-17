package ai.swim.sync;

import ai.swim.JniRunner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RAtomicBoolTest extends JniRunner {

  @Test
  void testAtomicBoolLoad() {
    RAtomicBool atomicBoolTrue = RAtomicBool.create(true);

    assertTrue(atomicBoolTrue.load(Ordering.Relaxed));

    RAtomicBool atomicBoolFalse = RAtomicBool.create(false);
    assertFalse(atomicBoolFalse.load(Ordering.Relaxed));
  }

  @Test
  void testAtomicBoolStore() {
    RAtomicBool atomicBool = RAtomicBool.create(true);
    assertTrue(atomicBool.load(Ordering.Relaxed));

    atomicBool.store(false, Ordering.Relaxed);
    assertFalse(atomicBool.load(Ordering.Relaxed));

    atomicBool.store(true, Ordering.Relaxed);
    assertTrue(atomicBool.load(Ordering.Relaxed));
  }

}
