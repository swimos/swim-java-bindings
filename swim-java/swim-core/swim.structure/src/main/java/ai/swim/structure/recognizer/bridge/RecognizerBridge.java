// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.recognizer.bridge;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.write.BodyWriter;
import ai.swim.structure.write.HeaderWriter;
import ai.swim.structure.write.StructuralWriter;
import ai.swim.structure.write.Writable;
import ai.swim.structure.write.header.WritableHeader;

import java.math.BigDecimal;
import java.math.BigInteger;

public class RecognizerBridge<T> implements StructuralWriter<T>, BodyWriter<T>, HeaderWriter<T> {
  private Recognizer<T> recognizer;
  private final Stack stack;

  private static final class Stack {
    private int stackDepth;

    void enter() {
      stackDepth += 1;
    }

    void exit() {
      stackDepth -= 1;
    }

    boolean isEmpty() {
      return stackDepth == 0;
    }
  }

  public RecognizerBridge(Recognizer<T> recognizer) {
    this.recognizer = recognizer;
    this.stack = new Stack();
  }

  @Override
  public T writeExtant() {
    return feed(ReadEvent.extant());
  }

  @Override
  public T writeInt(int value) {
    return feed(ReadEvent.number(value));
  }

  @Override
  public T writeLong(long value) {
    return feed(ReadEvent.number(value));
  }

  @Override
  public T writeFloat(float value) {
    return feed(ReadEvent.number(value));
  }

  @Override
  public T writeDouble(double value) {
    return feed(ReadEvent.number(value));
  }

  @Override
  public T writeBool(boolean value) {
    return feed(ReadEvent.bool(value));
  }

  @Override
  public T writeBigInt(BigInteger value) {
    return feed(ReadEvent.number(value));
  }

  @Override
  public T writeBigDecimal(BigDecimal value) {
    return feed(ReadEvent.number(value));
  }

  @Override
  public T writeText(String value) {
    return feed(ReadEvent.text(value));
  }

  @Override
  public T writeBlob(byte[] value) {
    return feed(ReadEvent.blob(value));
  }

  private T feed(ReadEvent event) {
    if (!stack.isEmpty()) {
      feedExpect(event);
      return null;
    } else {
      recognizer = recognizer.feedEvent(event);
      if (recognizer.isDone()) {
        return recognizer.bind();
      } else if (recognizer.isCont()) {
        throw RecognizerException.incomplete();
      } else {
        throw recognizer.trap();
      }
    }
  }

  private void feedExpect(ReadEvent event) {
    recognizer = recognizer.feedEvent(event);
    if (recognizer.isError()) {
      throw recognizer.trap();
    }
  }

  @Override
  public <K, V> BodyWriter<T> writeSlot(Writable<K> keyWriter, K key, Writable<V> valueWriter, V value) {
    stack.enter();
    keyWriter.writeInto(key, this);
    feedExpect(ReadEvent.slot());
    valueWriter.writeInto(value, this);
    stack.exit();
    return this;
  }

  @Override
  public <V> BodyWriter<T> writeValue(Writable<V> writer, V value) {
    stack.enter();
    writer.writeInto(value, this);
    stack.exit();
    return this;
  }

  @Override
  public T done() {
    if (stack.isEmpty()) {
      try {
        return feed(ReadEvent.endRecord());
      } catch (RecognizerException e) {
        T value = recognizer.flush();
        if (value == null) {
          throw e;
        } else {
          return value;
        }
      }
    } else {
      feedExpect(ReadEvent.endRecord());
      return null;
    }
  }

  @Override
  public HeaderWriter<T> record(int numAttrs) {
    return this;
  }

  @Override
  public HeaderWriter<T> writeExtantAttr(String key) {
    stack.enter();
    feedExpect(ReadEvent.startAttribute(key));
    feedExpect(ReadEvent.endAttribute());
    stack.exit();
    return this;
  }

  @Override
  public <V> HeaderWriter<T> writeAttr(String key, Writable<V> valueWriter, V value) {
    stack.enter();
    feedExpect(ReadEvent.startAttribute(key));
    valueWriter.writeInto(value, this);
    stack.exit();
    feedExpect(ReadEvent.endAttribute());
    return this;
  }

  @Override
  public HeaderWriter<T> writeAttr(String key, WritableHeader writable) {
    stack.enter();
    feedExpect(ReadEvent.startAttribute(key));
    writable.writeInto(this);
    feedExpect(ReadEvent.endAttribute());
    stack.exit();
    return this;
  }

  @Override
  public <V> T delegate(Writable<V> valueWriter, V value) {
    return valueWriter.writeInto(value, this);
  }

  @Override
  public BodyWriter<T> completeHeader(int numItems) {
    feedExpect(ReadEvent.startBody());
    return this;
  }
}
