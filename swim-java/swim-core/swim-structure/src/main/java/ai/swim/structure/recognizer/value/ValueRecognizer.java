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

package ai.swim.structure.recognizer.value;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.value.Item;
import ai.swim.structure.value.Record;
import ai.swim.structure.value.Text;
import ai.swim.structure.value.Value;
import ai.swim.structure.value.ValueItem;
import ai.swim.util.Either;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

public class ValueRecognizer extends Recognizer<Value> {
  private static final PrimitiveReadEventVisitor primitiveVisitor = new PrimitiveReadEventVisitor();
  protected final Deque<IncrementalValueBuilder> stack;
  private Value slotKey;

  public ValueRecognizer() {
    stack = new ArrayDeque<>(2);
  }

  @Override
  public Recognizer<Value> feedEvent(ReadEvent event) {
    try {
      return feed(event);
    } catch (NoSuchElementException e) {
      return Recognizer.error(new RuntimeException("Stack underflow", e));
    }
  }

  private Recognizer<Value> feed(ReadEvent event) {
    if (stack.isEmpty()) {
      if (event.isPrimitive()) {
        return Recognizer.done(event.visit(primitiveVisitor), this);
      } else if (event.isStartAttribute()) {
        pushAttrFrame((ReadStartAttribute) event);
        return this;
      } else if (event.isStartBody()) {
        pushRecordFrame(true);
        return this;
      } else {
        return Recognizer.error(new RuntimeException("Expected a primitive value, attribute or record start event"));
      }
    } else {
      if (event.isPrimitive()) {
        addItem(event.visit(primitiveVisitor));
      } else if (event.isStartAttribute()) {
        pushAttrFrame((ReadStartAttribute) event);
      } else if (event.isStartBody()) {
        pushRecordItem();
      } else if (event.isSlot()) {
        setSlotKey();
      } else if (event.isEndAttribute()) {
        return pop(true);
      } else if (event.isEndRecord()) {
        return pop(false);
      } else {
        throw new AssertionError("Unhandled read event type: " + event);
      }
    }

    return this;
  }

  @Override
  public Recognizer<Value> reset() {
    return new ValueRecognizer();
  }

  @Override
  public Recognizer<Value> asAttrRecognizer() {
    return new AttrBodyValueRecognizer();
  }

  @Override
  public Recognizer<Value> asBodyRecognizer() {
    return new DelegateBodyValueRecognizer();
  }

  @Override
  public Value flush() {
    if (stack.size() > 1) {
      return null;
    } else {
      IncrementalValueBuilder builder = stack.pollLast();
      if (builder == null) {
        return Value.extant();
      } else {
        if (builder.inBody) {
          return null;
        } else {
          return Value.ofAttrs(List.of(builder.record.build().getAttrs()));
        }
      }
    }
  }

  private void pushAttrFrame(ReadStartAttribute attribute) {
    IncrementalValueBuilder builder = stack.peekLast();
    if (!(builder != null && !builder.inBody)) {
      pushRecordFrame(false);
    }

    stack.addLast(new IncrementalValueBuilder(Either.left(Text.of(attribute.value())), true));
  }

  private void pushRecordFrame(boolean inBody) {
    Either<Text, Value> key = slotKey == null ? null : Either.right(slotKey);
    stack.addLast(new IncrementalValueBuilder(key, inBody));
    slotKey = null;
  }

  private void pushRecordItem() {
    IncrementalValueBuilder builder = stack.getLast();
    if (builder.inBody) {
      pushRecordFrame(true);
    } else {
      builder.inBody = true;
    }
  }

  private void setSlotKey() {
    IncrementalValueBuilder builder = stack.getLast();
    Item last = builder.record.popItem();
    if (last != null && !last.isSlot()) {
      slotKey = ((ValueItem) last).getValue();
    } else {
      slotKey = Value.extant();
    }
  }

  public Recognizer<Value> pop(boolean isAttrEnd) {
    IncrementalValueBuilder builder = stack.removeLast();
    Record.Builder recordBuilder = builder.record;
    if (builder.key == null) {
      if (isAttrEnd) {
        throw new RecognizerException("Inconsistent state");
      } else {
        Value record = recordBuilder.build();
        if (stack.isEmpty()) {
          return Recognizer.done(record, this);
        } else {
          stack.getLast().record.pushItem(record);
        }
      }
    } else {
      builder.key.peek(new Either.Peek<>() {
        @Override
        public void peekLeft(Text attr) {
          if (isAttrEnd) {
            if (recordBuilder.attrCount() == 0 && recordBuilder.itemCount() <= 1) {
              Item item = recordBuilder.popItem();
              if (item == null) {
                addAttr(attr, Value.extant());
              } else if (item.isSlot()) {
                addAttr(attr, Value.ofItems(List.of(item)));
              } else {
                addAttr(attr, ((ValueItem) item).getValue());
              }
            } else {
              addAttr(attr, recordBuilder.build());
            }
          } else {
            throw new RecognizerException("Inconsistent state");
          }
        }

        @Override
        public void peekRight(Value value) {
          if (isAttrEnd) {
            throw new RecognizerException("Inconsistent state");
          } else {
            stack.getLast().record.pushItem(builder.key.unwrapRight(), recordBuilder.build());
          }
        }
      });
    }

    return this;
  }

  private void addAttr(Text attr, Value item) {
    stack.getLast().record.pushAttr(attr, item);
  }

  private void addItem(Value value) {
    IncrementalValueBuilder builder = stack.getLast();
    if (builder.inBody) {
      if (slotKey != null) {
        builder.record.pushItem(slotKey, value);
        slotKey = null;
      } else {
        builder.record.pushItem(value);
      }
    }
  }

  @Override
  public String toString() {
    return "ValueRecognizer{" +
        "stack=" + stack +
        ", slotKey=" + slotKey +
        '}';
  }

  protected static class IncrementalValueBuilder {
    protected final Record.Builder record = new Record.Builder();
    protected final Either<Text, Value> key;
    protected boolean inBody;

    public IncrementalValueBuilder(Either<Text, Value> key, boolean inBody) {
      this.key = key;
      this.inBody = inBody;
    }

    @Override
    public String toString() {
      return "IncrementalValueBuilder{" +
          "record=" + record +
          ", key=" + key +
          ", inBody=" + inBody +
          '}';
    }
  }
}
