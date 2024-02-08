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

package ai.swim.server.lanes.map.codec;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.Size;
import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.ReadBufferInput;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.structure.FormParser;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
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

  public MapOperationDecoder(Recognizer<K> keyRecognizer, Recognizer<V> valueRecognizer) {
    this.keyRecognizer = keyRecognizer;
    this.valueRecognizer = valueRecognizer;
    this.state = State.ReadingHeader;
  }

  private static <T> T parseRecognise(ReadBuffer reader, int limit, Recognizer<T> recognizer) {
    ReadBufferInput readBufferInput = Input.readBuffer(reader).limit(reader.readPointer() + limit);
    FormParser<T> parser = new FormParser<>(recognizer.reset());
    Parser<T> parseResult = parser.feed(readBufferInput);

    if (parseResult.isDone()) {
      reader.advance(limit);
      return parseResult.bind();
    } else if (parseResult.isError()) {
      ParserError<T> parserError = (ParserError<T>) parseResult;
      String message = String.format("Parse error: %s at %s", parserError.cause(), parserError.location());
      throw new RecognizerException(message);
    } else {
      throw new RecognizerException("Unconsumed input by parser");
    }
  }

  @Override
  public Decoder<MapOperation<K, V>> decode(ReadBuffer buffer) throws DecoderException {
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
                  int valueLen = totalLen - keyLen - Size.LONG - Size.BYTE;

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

  @Override
  public Decoder<MapOperation<K, V>> reset() {
    return new MapOperationDecoder<>(keyRecognizer.reset(), valueRecognizer.reset());
  }

  private enum State {
    ReadingHeader, ReadingKey, AfterKey, ReadingValue
  }
}
