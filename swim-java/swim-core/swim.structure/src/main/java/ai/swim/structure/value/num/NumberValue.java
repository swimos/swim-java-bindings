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

package ai.swim.structure.value.num;


import ai.swim.structure.value.PrimitiveValue;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class NumberValue extends PrimitiveValue {
  public static NumberValue of(int value) {
    return new NumberI32(value);
  }

  public static NumberValue of(long value) {
    return new NumberI64(value);
  }

  public static NumberValue of(float value) {
    return new NumberF32(value);
  }

  public static NumberValue of(double value) {
    return new NumberF64(value);
  }

  public static NumberValue of(BigInteger value) {
    return new NumberBigInt(value);
  }

  public static NumberValue of(BigDecimal value) {
    return new NumberBigDecimal(value);
  }
}
