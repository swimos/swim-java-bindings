package ai.swim.server.lanes.models.response;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Encoder;
import java.util.Objects;
import java.util.UUID;

/**
 * An abstract lane response which may be either a command, sync, sync complete, or event response.
 *
 * @param <T> the response's event type.
 */
public abstract class LaneResponse<T> {
  public static final byte COMMAND = 0;
  public static final byte SYNC = 1;
  public static final byte SYNC_COMPLETE = 2;
  public static final byte EVENT = 3;
  public static final byte INIT_DONE = 4;
  public static final byte INITIALIZED = 5;
  public static final byte TAG_LEN = 1;
  public static final byte UUID_LEN = 16;

  public static <T> LaneResponse<T> initialized() {
    return new Initialized<>();
  }

  public static <T> LaneResponse<T> event(T event) {
    return new StandardEvent<>(event);
  }

  public static <T> LaneResponse<T> syncEvent(UUID remote, T event) {
    return new SyncEvent<>(remote, event);
  }

  public static <T> LaneResponse<T> synced(UUID remote) {
    return new Synced<>(remote);
  }

  /**
   * Encodes this response using {@code encoder} and into {@code buffer}.
   *
   * @param encoder for this response's event type.
   * @param buffer  to encode into.
   */
  public abstract void encode(Encoder<T> encoder, Bytes buffer);

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();

  private static class Initialized<T> extends LaneResponse<T> {
    @Override
    public void encode(Encoder<T> encoder, Bytes buffer) {
      buffer.writeByte(INITIALIZED);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      return o != null && getClass() == o.getClass();
    }

    @Override
    public String toString() {
      return "Initialized{}";
    }
  }

  private static class StandardEvent<T> extends LaneResponse<T> {
    private final T event;

    StandardEvent(T event) {
      this.event = event;
    }

    @Override
    public void encode(Encoder<T> encoder, Bytes buffer) {
      buffer.writeByte(EVENT);
      encoder.encode(event, buffer);
    }

    @Override
    public String toString() {
      return "StandardEvent{" +
          "event=" + event +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StandardEvent<?> that = (StandardEvent<?>) o;
      return Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
      return Objects.hash(event);
    }
  }

  private static class SyncEvent<T> extends LaneResponse<T> {
    private final T event;
    private final UUID remote;

    SyncEvent(UUID remote, T event) {
      this.remote = remote;
      this.event = event;
    }

    @Override
    public void encode(Encoder<T> encoder, Bytes dst) {
      dst.reserve(TAG_LEN + UUID_LEN);
      dst.writeByte(SYNC);
      dst.writeLong(remote.getMostSignificantBits());
      dst.writeLong(remote.getLeastSignificantBits());
      encoder.encode(event, dst);
    }

    @Override
    public String toString() {
      return "SyncEvent{" +
          "event=" + event +
          ", remote=" + remote +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SyncEvent<?> syncEvent = (SyncEvent<?>) o;
      return Objects.equals(event, syncEvent.event) && Objects.equals(remote, syncEvent.remote);
    }

    @Override
    public int hashCode() {
      return Objects.hash(event, remote);
    }
  }

  private static class Synced<T> extends LaneResponse<T> {
    private final UUID remote;

    Synced(UUID remote) {
      this.remote = remote;
    }

    @Override
    public void encode(Encoder<T> encoder, Bytes dst) {
      dst.reserve(TAG_LEN + UUID_LEN);
      dst.writeByte(SYNC_COMPLETE);
      dst.writeLong(remote.getMostSignificantBits());
      dst.writeLong(remote.getLeastSignificantBits());
    }

    @Override
    public String toString() {
      return "Synced{" +
          "remote=" + remote +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Synced<?> synced = (Synced<?>) o;
      return Objects.equals(remote, synced.remote);
    }

    @Override
    public int hashCode() {
      return Objects.hash(remote);
    }
  }
}
