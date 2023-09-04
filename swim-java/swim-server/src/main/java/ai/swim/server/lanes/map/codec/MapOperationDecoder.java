package ai.swim.server.lanes.map.codec;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.Size;
import ai.swim.codec.data.ByteReader;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.codec.input.ByteReaderInput;
import ai.swim.codec.input.Input;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.structure.FormParser;
import ai.swim.structure.recognizer.Recognizer;
import static ai.swim.server.lanes.map.MapOperation.CLEAR;
import static ai.swim.server.lanes.map.MapOperation.REMOVE;
import static ai.swim.server.lanes.map.MapOperation.UPDATE;

public class MapOperationDecoder<K, V> extends Decoder<MapOperation<K, V>> {
  private final Recognizer<K> keyRecognizer;
  private final Recognizer<V> valueRecognizer;
  private State state;
  private int remaining;
  private Integer valueSize;
  private K key;

  private enum State {
    ReadingHeader, ReadingKey, AfterKey, ReadingValue
  }

  public MapOperationDecoder(Recognizer<K> keyRecognizer, Recognizer<V> valueRecognizer) {
    this.keyRecognizer = keyRecognizer;
    this.valueRecognizer = valueRecognizer;
    this.state = State.ReadingHeader;
  }

  @Override
  public Decoder<MapOperation<K, V>> decode(ByteReader buffer) throws DecoderException {
    if (buffer.remaining() < Size.LONG + Size.BYTE) {
      return this;
    } else {
      while (true) {
        switch (state) {
          case ReadingHeader:
            int totalLen = longToInt(buffer.peekLong());
            byte tag = buffer.peekByte(Size.LONG);

            switch (tag) {
              case UPDATE:
                int required = Size.BYTE + 2 * Size.LONG;
                if (buffer.remaining() < required) {
                  return this;
                } else {
                  buffer.advance(Size.BYTE + Size.LONG);

                  int keyLen = longToInt(buffer.getLong());
                  int valueLen = totalLen - keyLen-Size.LONG-Size.BYTE;

                  if (valueLen <= 0) {
                    throw new DecoderException("Invalid header. Value length <= 0");
                  }

                  state = State.ReadingKey;
                  remaining = keyLen;
                  valueSize = valueLen;
                  continue;
                }
              case REMOVE:
                int keyLen = totalLen - Size.BYTE;
                if (keyLen <= 0) {
                  throw new DecoderException("Invalid header. Value length <= 0");
                }

                buffer.advance(Size.LONG + Size.BYTE);
                state = State.ReadingKey;
                remaining = keyLen;
                continue;
              case CLEAR:
                buffer.advance(Size.BYTE + Size.LONG);
                return Decoder.done(this, MapOperation.clear());
              default:
                throw new DecoderException(String.format("Unknown tag: %s", tag));
            }
          case ReadingKey:
            if (buffer.remaining() < remaining) {
              return this;
            } else {
              key = parseRecognise(buffer, remaining, keyRecognizer);
              state = State.AfterKey;
              break;
            }
          case AfterKey:
            if (valueSize != null) {
              state = State.ReadingValue;
              break;
            } else {
              return Decoder.done(this, MapOperation.remove(key));
            }
          case ReadingValue:
            return Decoder.done(this, MapOperation.update(key, parseRecognise(buffer, remaining, valueRecognizer)));
        }
      }
    }
  }

  private static <T> T parseRecognise(ByteReader reader, int limit, Recognizer<T> recognizer) throws DecoderException {
    ByteReaderInput byteReaderInput = Input.byteReader(reader).limit(reader.getReadPointer() + limit);
    FormParser<T> parser = new FormParser<>(recognizer.reset());
    Parser<T> parseResult = parser.feed(byteReaderInput);

    if (parseResult.isDone()) {
      reader.advance(limit);
      return parseResult.bind();
    } else if (parseResult.isError()) {
      ParserError<T> parserError = (ParserError<T>) parseResult;
      String message = String.format("Parser decode error: %s at %s", parserError.cause(), parserError.location());
      throw new DecoderException(message);
    } else {
      throw new DecoderException("Unconsumed input by parser");
    }
  }

  @Override
  public Decoder<MapOperation<K, V>> reset() {
    return new MapOperationDecoder<>(keyRecognizer.reset(), valueRecognizer.reset());
  }
}
