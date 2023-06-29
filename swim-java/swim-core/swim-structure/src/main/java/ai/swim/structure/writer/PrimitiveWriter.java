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

package ai.swim.structure.writer;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Base interface for structural writers to define how primitive values are written.
 *
 * @param <V> the type the writer produces.
 */
public interface PrimitiveWriter<V> {
  /**
   * Write an extant value.
   */
  V writeExtant();

  /**
   * Write an integer value.
   */
  V writeInt(int value);

  /**
   * Write a long value.
   */
  V writeLong(long value);

  /**
   * Write a float value.
   */
  V writeFloat(float value);

  /**
   * Write a double value.
   */
  V writeDouble(double value);

  /**
   * Write a boolean value.
   */
  V writeBool(boolean value);

  /**
   * Write a big integer value.
   */
  V writeBigInt(BigInteger value);

  /**
   * Write a big decimal value.
   */
  V writeBigDecimal(BigDecimal value);

  /**
   * Write a text value.
   */
  V writeText(String value);

  /**
   * Write a blob value.
   */
  V writeBlob(byte[] value);

}
