package ai.swim.ffi;

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
      this.setName("Pointer destructor task");
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
