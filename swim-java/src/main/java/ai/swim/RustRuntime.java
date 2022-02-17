package ai.swim;

public class RustRuntime {

  private static final long TOKIO_HANDLE;

  static {
    System.out.println(System.getProperty("user.dir"));

    System.loadLibrary("core_ffi");

    TOKIO_HANDLE = initRuntime();
  }

  private static native long initRuntime();

}
