// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.recon.event;

import ai.swim.codec.parsers.number.TypedNumber;
import ai.swim.recon.event.number.ReadBigDecimalValue;
import ai.swim.recon.event.number.ReadBigIntValue;
import ai.swim.recon.event.number.ReadDoubleValue;
import ai.swim.recon.event.number.ReadFloatValue;
import ai.swim.recon.event.number.ReadIntValue;
import ai.swim.recon.event.number.ReadLongValue;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.state.Action;
import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class ReadEvent {

  public static ReadEvent startAttribute(String name) {
    return new ReadStartAttribute(name);
  }

  public static ReadEvent extant() {
    return ReadExtant.INSTANCE;
  }

  public static ReadEvent blob(byte[] blob) {
    return new ReadBlobValue(blob);
  }

  public static ReadEvent bool(boolean bool) {
    return new ReadBooleanValue(bool);
  }

  public static ReadEvent endAttribute() {
    return ReadEndAttribute.INSTANCE;
  }

  public static ReadEvent startBody() {
    return ReadStartBody.INSTANCE;
  }

  public static ReadEvent text(String value) {
    return new ReadTextValue(value);
  }

  public static ReadEvent number(int value) {
    return new ReadIntValue(value);
  }

  public static ReadEvent number(long value) {
    return new ReadLongValue(value);
  }

  public static ReadEvent number(float value) {
    return new ReadFloatValue(value);
  }

  public static ReadEvent number(double value) {
    return new ReadDoubleValue(value);
  }

  public static ReadEvent number(BigInteger value) {
    return new ReadBigIntValue(value);
  }

  public static ReadEvent number(BigDecimal value) {
    return new ReadBigDecimalValue(value);
  }

  public static ReadEvent number(TypedNumber value) {
    if (value.isInt()) {
      return new ReadIntValue(value.intValue());
    } else if (value.isLong()) {
      return new ReadLongValue(value.longValue());
    } else if (value.isFloat()) {
      return new ReadFloatValue(value.floatValue());
    } else if (value.isDouble()) {
      return new ReadDoubleValue(value.doubleValue());
    } else if (value.isBigInt()) {
      return new ReadBigIntValue(value.bigIntValue());
    } else if (value.isBigDecimal()) {
      return new ReadBigDecimalValue(value.bigDecimalValue());
    } else {
      throw new AssertionError(value);
    }
  }

  public static ReadEvent slot() {
    return ReadSlot.INSTANCE;
  }

  public static ReadEvent endRecord() {
    return ReadEndRecord.INSTANCE;
  }

  public boolean isExtant() {
    return false;
  }

  public boolean isText() {
    return false;
  }

  public boolean isReadInt() {
    return false;
  }

  public boolean isReadLong() {
    return false;
  }

  public boolean isReadFloat() {
    return false;
  }

  public boolean isReadDouble() {
    return false;
  }

  public boolean isReadBigInt() {
    return false;
  }

  public boolean isReadBigDecimal() {
    return false;
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isBlob() {
    return false;
  }

  public boolean isStartAttribute() {
    return false;
  }

  public boolean isEndAttribute() {
    return false;
  }

  public boolean isStartBody() {
    return false;
  }

  public boolean isSlot() {
    return false;
  }

  public boolean isEndRecord() {
    return false;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  public ParserTransition transition() {
    return new ParserTransition(this, Action.none());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return o != null && getClass() == o.getClass();
  }

  public boolean isPrimitive() {
    return false;
  }

  public abstract <O> O visit(ReadEventVisitor<O> visitor);
}
