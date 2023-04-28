/// THIS FILE IS AUTOMATICALLY GENERATED BY THE BYTE BRIDGE LIBRARY.
/// ANY CHANGES MADE MAY BE LOST.
package ai.swim;

public class Test {

  /**
   * Timeout in seconds. If the runtime has no consumers for longer than this timeout, it will stop.
   */
  private int runtimeEmptyTimeout = 30;

  /**
   * Size of the queue for accepting new subscribers to a downlink.
   */
  private long runtimeAttachmentQueueSize = 16;

  /**
   * Abort the downlink on receiving invalid frames.
   */
  private boolean runtimeAbortOnBadFrames = true;

  /**
   * Size of the buffers to communicated with the socket.
   */
  private long runtimeRemoteBufferSize = 4096;

  /**
   * Size of the buffers to communicate with the downlink implementation.
   */
  private long runtimeDownlinkBufferSize = 4096;

  /**
   * Whether to trigger event handlers if the downlink receives events before it has
   * synchronized.
   */
  private boolean downlinkEventsWhenNotSynced = false;

  /**
   * Whether the downlink should terminate on an unlinked message.
   */
  private boolean downlinkTerminateOnUnlinked = true;

  /**
   * Downlink event buffer capacity.
   */
  private long downlinkBufferSize = 1024;

  /**
   * If the connection fails, it should be restarted and the consumer passed to the new
   * connection.
   */
  private boolean keepLinked = true;

  /**
   * Gets runtimeEmptyTimeout.
   * <p>
   * Default value: 30.
   *
   * @return runtimeEmptyTimeout
   */
  public int getRuntimeEmptyTimeout() {
    return this.runtimeEmptyTimeout;
  }

  /**
   * Sets the new runtimeEmptyTimeout.
   *
   * @param runtimeEmptyTimeout the new runtimeEmptyTimeout
   * @throws IllegalArgumentException if runtimeEmptyTimeout is negative
   */
  public void setRuntimeEmptyTimeout(int runtimeEmptyTimeout) {
    if (runtimeEmptyTimeout < 0) {
      throw new IllegalArgumentException("'runtimeEmptyTimeout' must be positive");
    }
    this.runtimeEmptyTimeout = runtimeEmptyTimeout;
  }

  /**
   * Gets runtimeAttachmentQueueSize.
   * <p>
   * Default value: 16.
   *
   * @return runtimeAttachmentQueueSize
   */
  public long getRuntimeAttachmentQueueSize() {
    return this.runtimeAttachmentQueueSize;
  }

  /**
   * Sets the new runtimeAttachmentQueueSize.
   *
   * @param runtimeAttachmentQueueSize the new runtimeAttachmentQueueSize
   * @throws IllegalArgumentException If 'runtimeAttachmentQueueSize' is not a natural number (< 1).
   */
  public void setRuntimeAttachmentQueueSize(long runtimeAttachmentQueueSize) {
    if (runtimeAttachmentQueueSize < 1) {
      throw new IllegalArgumentException("'runtimeAttachmentQueueSize' must be a natural number");
    }
    this.runtimeAttachmentQueueSize = runtimeAttachmentQueueSize;
  }

  /**
   * Gets runtimeAbortOnBadFrames.
   * <p>
   * Default value: true.
   *
   * @return runtimeAbortOnBadFrames
   */
  public boolean getRuntimeAbortOnBadFrames() {
    return this.runtimeAbortOnBadFrames;
  }

  /**
   * Sets the new runtimeAbortOnBadFrames.
   *
   * @param runtimeAbortOnBadFrames the new runtimeAbortOnBadFrames
   */
  public void setRuntimeAbortOnBadFrames(boolean runtimeAbortOnBadFrames) {
    this.runtimeAbortOnBadFrames = runtimeAbortOnBadFrames;
  }

  /**
   * Gets runtimeRemoteBufferSize.
   * <p>
   * Default value: 4096.
   *
   * @return runtimeRemoteBufferSize
   */
  public long getRuntimeRemoteBufferSize() {
    return this.runtimeRemoteBufferSize;
  }

  /**
   * Sets the new runtimeRemoteBufferSize.
   *
   * @param runtimeRemoteBufferSize the new runtimeRemoteBufferSize
   * @throws IllegalArgumentException If 'runtimeRemoteBufferSize' is not a natural number (< 1).
   */
  public void setRuntimeRemoteBufferSize(long runtimeRemoteBufferSize) {
    if (runtimeRemoteBufferSize < 1) {
      throw new IllegalArgumentException("'runtimeRemoteBufferSize' must be a natural number");
    }
    this.runtimeRemoteBufferSize = runtimeRemoteBufferSize;
  }

  /**
   * Gets runtimeDownlinkBufferSize.
   * <p>
   * Default value: 4096.
   *
   * @return runtimeDownlinkBufferSize
   */
  public long getRuntimeDownlinkBufferSize() {
    return this.runtimeDownlinkBufferSize;
  }

  /**
   * Sets the new runtimeDownlinkBufferSize.
   *
   * @param runtimeDownlinkBufferSize the new runtimeDownlinkBufferSize
   * @throws IllegalArgumentException If 'runtimeDownlinkBufferSize' is not a natural number (< 1).
   */
  public void setRuntimeDownlinkBufferSize(long runtimeDownlinkBufferSize) {
    if (runtimeDownlinkBufferSize < 1) {
      throw new IllegalArgumentException("'runtimeDownlinkBufferSize' must be a natural number");
    }
    this.runtimeDownlinkBufferSize = runtimeDownlinkBufferSize;
  }

  /**
   * Gets downlinkEventsWhenNotSynced.
   * <p>
   * Default value: false.
   *
   * @return downlinkEventsWhenNotSynced
   */
  public boolean getDownlinkEventsWhenNotSynced() {
    return this.downlinkEventsWhenNotSynced;
  }

  /**
   * Sets the new downlinkEventsWhenNotSynced.
   *
   * @param downlinkEventsWhenNotSynced the new downlinkEventsWhenNotSynced
   */
  public void setDownlinkEventsWhenNotSynced(boolean downlinkEventsWhenNotSynced) {
    this.downlinkEventsWhenNotSynced = downlinkEventsWhenNotSynced;
  }

  /**
   * Gets downlinkTerminateOnUnlinked.
   * <p>
   * Default value: true.
   *
   * @return downlinkTerminateOnUnlinked
   */
  public boolean getDownlinkTerminateOnUnlinked() {
    return this.downlinkTerminateOnUnlinked;
  }

  /**
   * Sets the new downlinkTerminateOnUnlinked.
   *
   * @param downlinkTerminateOnUnlinked the new downlinkTerminateOnUnlinked
   */
  public void setDownlinkTerminateOnUnlinked(boolean downlinkTerminateOnUnlinked) {
    this.downlinkTerminateOnUnlinked = downlinkTerminateOnUnlinked;
  }

  /**
   * Gets downlinkBufferSize.
   * <p>
   * Default value: 1024.
   *
   * @return downlinkBufferSize
   */
  public long getDownlinkBufferSize() {
    return this.downlinkBufferSize;
  }

  /**
   * Sets the new downlinkBufferSize.
   *
   * @param downlinkBufferSize the new downlinkBufferSize
   * @throws IllegalArgumentException If 'downlinkBufferSize' is not a natural number (< 1).
   */
  public void setDownlinkBufferSize(long downlinkBufferSize) {
    if (downlinkBufferSize < 1) {
      throw new IllegalArgumentException("'downlinkBufferSize' must be a natural number");
    }
    this.downlinkBufferSize = downlinkBufferSize;
  }

  /**
   * Gets keepLinked.
   * <p>
   * Default value: true.
   *
   * @return keepLinked
   */
  public boolean getKeepLinked() {
    return this.keepLinked;
  }

  /**
   * Sets the new keepLinked.
   *
   * @param keepLinked the new keepLinked
   */
  public void setKeepLinked(boolean keepLinked) {
    this.keepLinked = keepLinked;
  }

  /**
   * Returns a byte array representation of the current configuration.
   */
  public byte[] asBytes() {
    int __buf__size = 0;
    __buf__size += 40;
    java.nio.ByteBuffer __buf = java.nio.ByteBuffer.allocate(__buf__size);
    __buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    __buf.putInt(this.runtimeEmptyTimeout);
    __buf.putLong(this.runtimeAttachmentQueueSize);
    __buf.put((byte) (this.runtimeAbortOnBadFrames ? 1 : 0));
    __buf.putLong(this.runtimeRemoteBufferSize);
    __buf.putLong(this.runtimeDownlinkBufferSize);
    __buf.put((byte) (this.downlinkEventsWhenNotSynced ? 1 : 0));
    __buf.put((byte) (this.downlinkTerminateOnUnlinked ? 1 : 0));
    __buf.putLong(this.downlinkBufferSize);
    __buf.put((byte) (this.keepLinked ? 1 : 0));
    return __buf.array();
  }

}
