package ai.swim.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A single-use concurrent barrier for synchronising threads.
 * <p>
 * Any thread may trigger the barrier, but it may only happen once and the barrier may not be reset.
 */
public class Trigger {
  private final Lock lock;
  private final Condition condition;
  private volatile boolean triggered;

  public Trigger() {
    lock = new ReentrantLock();
    condition = lock.newCondition();
    triggered = false;
  }

  /**
   * Triggers the barrier and wakes up all waiting threads.
   */
  public void trigger() {
    lock.lock();

    if (!triggered) {
      condition.signalAll();
      triggered = true;
    }

    lock.unlock();
  }

  /**
   * Causes the current thread to wait until a trigger occurs, or it is {@linkplain Thread#interrupt interrupted}.
   *
   * @throws InterruptedException if the current thread is interrupted
   */
  public void awaitTrigger() throws InterruptedException {
    lock.lock();
    if (!triggered) {
      condition.await();
    }
    lock.unlock();
  }

  /**
   * Causes the current thread to wait until for a trigger or until it is signalled or interrupted, or the specified
   * waiting time elapses.
   *
   * @param time the maximum time to wait for a trigger event
   * @param unit the time unit of the {@code time} argument
   * @return {@code false} if the waiting time detectably elapsed before return from the method, else {@code true}
   * @throws InterruptedException if the current thread is interrupted
   */
  public boolean awaitTrigger(long time, TimeUnit unit) throws InterruptedException {
    lock.lock();

    if (!triggered) {
      return condition.await(time, unit);
    }

    return true;
  }

  /**
   * Returns whether a trigger event has occurred.
   */
  public boolean hasTriggered() {
    lock.lock();
    boolean triggered = this.triggered;
    lock.unlock();
    return triggered;
  }

}
