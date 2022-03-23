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

import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.state.NoStateChange;

public abstract class ReadEvent {

  public static ReadEvent startAttribute(String name) {
    return new ReadStartAttribute(name);
  }

  public static ReadEvent extant() {
    return new ReadExtant();
  }

  public static ReadEvent blob(byte[] blob) {
    return new ReadBlobValue(blob);
  }

  public static ReadEvent bool(boolean bool) {
    return new ReadBooleanValue(bool);
  }

  public static ReadEvent endAttribute() {
    return new ReadEndAttribute();
  }

  public static ReadEvent startBody() {
    return new ReadStartBody();
  }

  public static ReadEvent text(String value) {
    return new ReadTextValue(value);
  }

  public static ReadEvent number(Number value) {
    return new ReadNumberValue(value);
  }

  public static ReadEvent slot() {
    return new ReadSlot();
  }

  public static ReadEvent endRecord() {
    return new ReadEndRecord();
  }

  public boolean isExtant() {
    return false;
  }

  public boolean isText() {
    return false;
  }

  public boolean isNumber() {
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
    return new ParserTransition(this, new NoStateChange());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return o != null && getClass() == o.getClass();
  }
}
