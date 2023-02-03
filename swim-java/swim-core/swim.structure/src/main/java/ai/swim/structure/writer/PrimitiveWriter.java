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

public interface PrimitiveWriter<V> {
  V writeExtant();

  V writeInt(int value);

  V writeLong(long value);

  V writeFloat(float value);

  V writeDouble(double value);

  V writeBool(boolean value);

  V writeBigInt(BigInteger value);

  V writeBigDecimal(BigDecimal value);

  V writeText(String value);

  V writeBlob(byte[] value);

}
