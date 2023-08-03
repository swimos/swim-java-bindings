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

package ai.swim.structure.writer.value;

import ai.swim.structure.value.Value;
import ai.swim.structure.writer.HeaderWriter;
import ai.swim.structure.writer.StructuralWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class ValueStructuralWriter implements StructuralWriter<Value> {
  @Override
  public Value writeExtant() {
    return Value.extant();
  }

  @Override
  public Value writeInt(int value) {
    return Value.of(value);
  }

  @Override
  public Value writeLong(long value) {
    return Value.of(value);
  }

  @Override
  public Value writeFloat(float value) {
    return Value.of(value);
  }

  @Override
  public Value writeDouble(double value) {
    return Value.of(value);
  }

  @Override
  public Value writeBool(boolean value) {
    return Value.of(value);
  }

  @Override
  public Value writeBigInt(BigInteger value) {
    return Value.of(value);
  }

  @Override
  public Value writeBigDecimal(BigDecimal value) {
    return Value.of(value);
  }

  @Override
  public Value writeText(String value) {
    return Value.of(value);
  }

  @Override
  public Value writeBlob(byte[] value) {
    return Value.of(value);
  }

  @Override
  public HeaderWriter<Value> record(int numAttrs) {
    return new ValueInterpreter(this, numAttrs);
  }
}
