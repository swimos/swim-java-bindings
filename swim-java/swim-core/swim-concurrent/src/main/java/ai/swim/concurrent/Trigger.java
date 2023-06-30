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

    try {
      if (!triggered) {
        condition.signalAll();
        triggered = true;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Causes the current thread to wait until a trigger occurs, or it is {@linkplain Thread#interrupt interrupted}.
   *
   * @throws InterruptedException if the current thread is interrupted
   */
  public void awaitTrigger() throws InterruptedException {
    lock.lock();

    try {
      if (!triggered) {
        condition.await();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Causes the current thread to wait until for a trigger or until it is signaled or interrupted, or the specified
   * waiting time elapses.
   *
   * @param time the maximum time to wait for a trigger event
   * @param unit the time unit of the {@code time} argument
   * @return {@code false} if the waiting time detectably elapsed before return from the method, else {@code true}
   * @throws InterruptedException if the current thread is interrupted
   */
  public boolean awaitTrigger(long time, TimeUnit unit) throws InterruptedException {
    lock.lock();

    try {
      return triggered || condition.await(time, unit);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns whether a trigger event has occurred.
   */
  public boolean hasTriggered() {
    lock.lock();
    try {
      return triggered;
    } finally {
      lock.unlock();
    }
  }

}
