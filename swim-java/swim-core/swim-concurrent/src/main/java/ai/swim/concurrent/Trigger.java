/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
