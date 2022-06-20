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

package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;

import java.util.HashMap;

public class HashMapRecognizer<K, V> extends Recognizer<HashMap<K, V>> {

  private Recognizer<K> keyRecognizer;
  private Recognizer<V> valueRecognizer;
  private State state;
  private final boolean isAttrBody;
  private K key;
  private final HashMap<K, V> map;

  private enum State {
    Init,
    Between,
    Key,
    Slot,
    Value,
  }

  public HashMapRecognizer(Recognizer<K> keyRecognizer, Recognizer<V> valueRecognizer) {
    this(keyRecognizer, valueRecognizer, false);
  }

  public HashMapRecognizer(Recognizer<K> keyRecognizer, Recognizer<V> valueRecognizer, boolean isAttrBody) {
    this.keyRecognizer = keyRecognizer;
    this.valueRecognizer = valueRecognizer;
    this.state = isAttrBody ? State.Between : State.Init;
    this.isAttrBody = isAttrBody;
    this.map = new HashMap<>();
  }

  @Override
  public Recognizer<HashMap<K, V>> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Init:
        if (event.isStartBody()) {
          this.state = State.Between;
          return this;
        } else {
          return Recognizer.error(ReadEvent.startBody(), event);
        }
      case Between:
        if (event.isEndRecord() && !this.isAttrBody) {
          return Recognizer.done(this.map, this);
        } else if (event.isEndAttribute() && this.isAttrBody) {
          return Recognizer.done(this.map, this);
        } else {
          this.state = State.Key;
          return onKey(event);
        }
      case Key:
        return onKey(event);
      case Slot:
        if (event.isSlot()) {
          this.state = State.Value;
          return this;
        } else {
          return Recognizer.error(ReadEvent.slot(), event);
        }
      case Value:
        this.valueRecognizer = this.valueRecognizer.feedEvent(event);

        if (this.valueRecognizer.isDone()) {
          this.map.put(this.key, this.valueRecognizer.bind());
          this.key = null;
          this.valueRecognizer = this.valueRecognizer.reset();
          this.state = State.Between;
          return this;
        } else if (this.valueRecognizer.isError()) {
          return Recognizer.error(this.valueRecognizer.trap());
        } else if (this.valueRecognizer.isCont()) {
          return this;
        } else {
          throw new AssertionError();
        }
      default:
        throw new AssertionError(this.state);
    }
  }

  private Recognizer<HashMap<K, V>> onKey(ReadEvent event) {
    this.keyRecognizer = this.keyRecognizer.feedEvent(event);

    if (this.keyRecognizer.isDone()) {
      this.key = this.keyRecognizer.bind();
      this.keyRecognizer = this.keyRecognizer.reset();
      this.state = State.Slot;

      return this;
    } else if (this.keyRecognizer.isCont()) {
      return this;
    } else if (this.keyRecognizer.isError()) {
      return Recognizer.error(this.keyRecognizer.trap());
    } else {
      throw new AssertionError();
    }
  }

  @Override
  public Recognizer<HashMap<K, V>> reset() {
    return new HashMapRecognizer<>(this.keyRecognizer.reset(), this.valueRecognizer.reset(), this.isAttrBody);
  }

}
