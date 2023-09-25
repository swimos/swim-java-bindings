package ai.swim.concurrent;

import org.junit.jupiter.api.Test;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerTest {

  @Test
  void testTrigger() throws InterruptedException {
    Trigger trigger = new Trigger();
    CyclicBarrier barrier = new CyclicBarrier(3);

    assertFalse(trigger.hasTriggered());

    Thread waiter1 = new Thread(() -> {
      try {
        barrier.await();
        trigger.awaitTrigger();
        assertTrue(trigger.hasTriggered());
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
    });
    Thread waiter2 = new Thread(() -> {
      try {
        barrier.await();
        trigger.awaitTrigger();
        assertTrue(trigger.hasTriggered());
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
    });
    Thread triggerThread = new Thread(() -> {
      try {
        barrier.await();
        trigger.trigger();
        assertTrue(trigger.hasTriggered());
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
    });

    waiter1.start();
    waiter2.start();
    triggerThread.start();

    waiter1.join();
    waiter2.join();
    triggerThread.join();
  }

  @Test
  void awaitTimeout() throws InterruptedException {
    Trigger trigger = new Trigger();
    CyclicBarrier barrier = new CyclicBarrier(2);

    assertFalse(trigger.hasTriggered());

    Thread waiter = new Thread(() -> {
      try {
        barrier.await();

        assertFalse(trigger.awaitTrigger(5, TimeUnit.SECONDS));
        barrier.await();

        trigger.awaitTrigger();
        assertTrue(trigger.hasTriggered());
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
    });
    Thread triggerThread = new Thread(() -> {
      try {
        barrier.await(); // Startup barrier
        barrier.reset(); // We want to reset the barrier while the waiter is awaiting the trigger it's not broken
        barrier.await(); // Waiter thread timeout barrier

        trigger.trigger();
        assertTrue(trigger.hasTriggered());
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
    });

    waiter.start();
    triggerThread.start();

    waiter.join();
    triggerThread.join();
  }
}