package ai.swim.server.lanes.map;

import ai.swim.codec.Size;
import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.EncoderException;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.print.StructurePrinter;
import ai.swim.structure.writer.print.strategy.PrintStrategy;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class MapOperation<K, V> {
  public static final byte UPDATE = 0;
  public static final byte REMOVE = 1;
  public static final byte CLEAR = 2;

  public static <K, V> MapOperation<K, V> update(K key, V value) {
    return new Update<>(key, value);
  }

  public static <K, V> MapOperation<K, V> remove(K key) {
    return new Remove<>(key);
  }

  public static <K, V> MapOperation<K, V> clear() {
    return new Clear<>();
  }

  public abstract void encode(Writable<K> kEncoder, Writable<V> vEncoder, ByteWriter buffer);

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();

  public abstract void accept(MapOperationVisitor<K, V> visitor);

  // Byte layout:
  //  Total length
  //  UPDATE tag
  //  Key length
  //  Key data
  //  Value data
  private static class Update<K, V> extends MapOperation<K, V> {
    private final K key;
    private final V value;

    public Update(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public void encode(Writable<K> kEncoder, Writable<V> vEncoder, ByteWriter buffer) {
      buffer.reserve(2 * Size.LONG + Size.BYTE);

      int startPosition = buffer.writePosition();
      buffer.writeLong(0);

      int startLen = buffer.writePosition();
      buffer.writeByte(UPDATE);

      int keyPosition = buffer.writePosition();
      buffer.writeLong(0);
      int keyStart = buffer.writePosition();

      try (OutputStreamWriter writer = new OutputStreamWriter(buffer.outputStream(), StandardCharsets.UTF_8)) {
        kEncoder.writeInto(key, new StructurePrinter(writer, PrintStrategy.COMPACT));
        writer.flush();

        int keyLen = buffer.writePosition() - keyStart;

        vEncoder.writeInto(value, new StructurePrinter(writer, PrintStrategy.COMPACT));
        writer.flush();

        int totalLen = buffer.writePosition() - startLen;

        buffer.writeLong(totalLen, startPosition);
        buffer.writeLong(keyLen, keyPosition);
      } catch (IOException e) {
        throw new EncoderException(e);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Update<?, ?> update = (Update<?, ?>) o;
      return Objects.equals(key, update.key) && Objects.equals(value, update.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, value);
    }

    @Override
    public String toString() {
      return "Update{" +
          "key=" + key +
          ", value=" + value +
          '}';
    }

    @Override
    public void accept(MapOperationVisitor<K, V> visitor) {
      visitor.visitUpdate(key, value);
    }
  }

  // Byte layout:
  //  Total length
  //  REMOVE tag
  //  Key data
  private static class Remove<K, V> extends MapOperation<K, V> {
    private final K key;

    public Remove(K key) {
      this.key = key;
    }

    @Override
    public void encode(Writable<K> kEncoder, Writable<V> vEncoder, ByteWriter buffer) {
      buffer.reserve(Size.LONG + Size.BYTE);

      int startLen = buffer.writePosition();
      buffer.writeLong(0);
      int startPosition = buffer.writePosition();
      buffer.writeByte(REMOVE);

      try (OutputStreamWriter writer = new OutputStreamWriter(buffer.outputStream(), StandardCharsets.UTF_8)) {
        kEncoder.writeInto(key, new StructurePrinter(writer, PrintStrategy.COMPACT));
      } catch (IOException e) {
        throw new EncoderException(e);
      }

      int totalLen = buffer.writePosition() - startPosition;

      buffer.writeLong(totalLen, startLen);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Remove<?, ?> remove = (Remove<?, ?>) o;
      return Objects.equals(key, remove.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key);
    }

    @Override
    public String toString() {
      return "Remove{" +
          "key=" + key +
          '}';
    }

    @Override
    public void accept(MapOperationVisitor<K, V> visitor) {
      visitor.visitRemove(key);
    }
  }

  // Byte layout:
  //  Total length
  //  CLEAR tag
  private static class Clear<K, V> extends MapOperation<K, V> {
    @Override
    public void encode(Writable<K> kEncoder, Writable<V> vEncoder, ByteWriter buffer) {
      buffer.writeLong(Size.BYTE);
      buffer.writeByte(CLEAR);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public String toString() {
      return "Clear{}";
    }

    @Override
    public void accept(MapOperationVisitor<K, V> visitor) {
      visitor.visitClear();
    }

  }
}
