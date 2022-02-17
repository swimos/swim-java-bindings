package ai.swim.io;

import ai.swim.JniRunner;
import ai.swim.ffi.FfiIntrinsic;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;

class RByteReaderTest extends JniRunner {

  private static CountDownLatch readLatch = new CountDownLatch(6);
  private static CountDownLatch closeLatch = new CountDownLatch(1);

  @Test
  void read() throws InterruptedException {
    long now = System.nanoTime();

    RByteReader rByteReader = RByteReader.create(64,
        new DidReadImpl(),
        new DidCloseImpl()
    );

    readLatch.await();
    closeLatch.await();
    System.out.println(System.nanoTime() - now);
  }

  static class DidReadImpl implements RByteReader.DidReadCallback {
    @Override
    @FfiIntrinsic
    public void didRead(byte[] bytes) {
      readLatch.countDown();
    }
  }

  static class DidCloseImpl implements RByteReader.DidCloseCallback {
    @Override
    @FfiIntrinsic
    public void didClose() {
      System.out.println("Reader closed");
      closeLatch.countDown();
    }
  }

}