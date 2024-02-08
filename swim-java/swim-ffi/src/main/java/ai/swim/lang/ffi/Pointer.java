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

package ai.swim.lang.ffi;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Pointer {

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private static final ConcurrentLinkedQueue<Destructor> destructorStack;
  private static final ReferenceQueue<NativeResource> referenceQueue;
  private static final Thread destructorTask;

  static {
    destructorStack = new ConcurrentLinkedQueue<>();
    referenceQueue = new ReferenceQueue<>();

    destructorTask = new DestructorTask();
    destructorTask.start();
  }

  public static Destructor newDestructor(NativeResource referent, Destruct callback) {
    return new Destructor(referent, callback);
  }

  @FunctionalInterface
  public interface Destruct {
    void call();
  }

  public static final class Destructor extends PhantomReference<NativeResource> {
    private Destruct callback;

    private Destructor(NativeResource referent, Destruct callback) {
      super(referent, referenceQueue);
      destructorStack.add(this);
      this.callback = callback;
    }

    private void destruct() {
      if (this.callback != null) {
        this.callback.call();
        this.callback = null;
      }
    }
  }

  private static class DestructorTask extends Thread {
    public DestructorTask() {
      this.setPriority(Thread.MAX_PRIORITY);
      this.setDaemon(true);
      this.setName("Native resource reclamation task");
    }

    @Override
    public void run() {
      while (true) {
        try {
          Destructor current = (Destructor) referenceQueue.remove();
          current.destruct();

          destructorStack.remove(current);
        } catch (InterruptedException ignored) {

        }
      }
    }
  }

}