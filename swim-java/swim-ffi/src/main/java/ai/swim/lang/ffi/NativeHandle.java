package ai.swim.lang.ffi;

/**
 * A native resource handle that can be provided to classes that are not concerned about their implementation details,
 * only that the resource is closed correctly.
 * <p>
 * This is useful in instances where functionality is shared across both a client and server but have slightly different
 * implementation details.
 */
public interface NativeHandle extends NativeResource, AutoCloseable {
  /**
   * Returns a pointer to the native resource.
   */
  long get();

  /**
   * Drop the native resource.
   */
  void drop();

  /**
   * Drop the native resource.
   */
  @Override
  void close();
}
