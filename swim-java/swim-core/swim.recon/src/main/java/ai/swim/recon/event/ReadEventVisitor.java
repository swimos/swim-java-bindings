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

package ai.swim.recon.event;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface ReadEventVisitor<O> {
  default O visitBigDecimal(BigDecimal value) {
    throw new UnsupportedOperationException();
  }

  default O visitBigInt(BigInteger value) {
    throw new UnsupportedOperationException();
  }

  default O visitDouble(double value) {
    throw new UnsupportedOperationException();
  }

  default O visitFloat(float value) {
    throw new UnsupportedOperationException();
  }

  default O visitInt(int value) {
    throw new UnsupportedOperationException();
  }

  default O visitLong(long value) {
    throw new UnsupportedOperationException();
  }

  default O visitBlob(byte[] value) {
    throw new UnsupportedOperationException();
  }

  default O visitBoolean(boolean value) {
    throw new UnsupportedOperationException();
  }

  default O visitText(String value) {
    throw new UnsupportedOperationException();
  }

  default O visitStartAttribute() {
    throw new UnsupportedOperationException();
  }

  default O visitEndAttribute() {
    throw new UnsupportedOperationException();
  }

  default O visitStartBody() {
    throw new UnsupportedOperationException();
  }

  default O visitEndRecord() {
    throw new UnsupportedOperationException();
  }

  default O visitExtant() {
    throw new UnsupportedOperationException();
  }

  default O visitSlot() {
    throw new UnsupportedOperationException();
  }
}
