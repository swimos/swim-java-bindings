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

package ai.swim.structure.recognizer.value;

import ai.swim.recon.event.ReadEventVisitor;
import ai.swim.structure.value.Value;
import java.math.BigDecimal;
import java.math.BigInteger;

public class PrimitiveReadEventVisitor implements ReadEventVisitor<Value> {
  @Override
  public Value visitBigDecimal(BigDecimal value) {
    return Value.of(value);
  }

  @Override
  public Value visitBigInt(BigInteger value) {
    return Value.of(value);
  }

  @Override
  public Value visitDouble(double value) {
    return Value.of(value);
  }

  @Override
  public Value visitFloat(float value) {
    return Value.of(value);
  }

  @Override
  public Value visitInt(int value) {
    return Value.of(value);
  }

  @Override
  public Value visitLong(long value) {
    return Value.of(value);
  }

  @Override
  public Value visitBlob(byte[] value) {
    return Value.of(value);
  }

  @Override
  public Value visitBoolean(boolean value) {
    return Value.of(value);
  }

  @Override
  public Value visitText(String value) {
    return Value.of(value);
  }
}
