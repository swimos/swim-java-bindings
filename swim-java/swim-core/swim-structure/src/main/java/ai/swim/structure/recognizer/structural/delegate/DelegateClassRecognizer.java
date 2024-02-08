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

package ai.swim.structure.recognizer.structural.delegate;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.ClassRecognizer;
import ai.swim.structure.recognizer.structural.IndexFn;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class DelegateClassRecognizer<T> extends ClassRecognizer<DelegateClassRecognizer.State, OrdinalFieldKey, T> {
  public DelegateClassRecognizer(TagSpec tagSpec,
      RecognizingBuilder<T> builder,
      int fieldCount,
      IndexFn<OrdinalFieldKey> indexFn) {
    super(tagSpec, builder, fieldCount, indexFn, State.Init);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (state) {
      case Init:
        return onInit(event, OrdinalFieldKey.HEADER);
      case Header:
        return onHeader(event, State.AttrBetween);
      case NoHeader:
        return onNoHeader(event, State.AttrBetween);
      case AttrBetween:
        return onAttrBetween(event);
      case AttrItem:
        return onAttrItem(event, State.AttrBetween);
      case Delegated:
        return onDelegated(event);
      default:
        throw new AssertionError(state);
    }
  }

  private Recognizer<T> onDelegated(ReadEvent event) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        return Recognizer.done(this.builder.bind(), this);
      } else {
        return this;
      }
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }

  private Recognizer<T> onAttrBetween(ReadEvent event) {
    if (event.isStartBody()) {
      Integer idx = this.indexFn.selectIndex(OrdinalFieldKey.FIRST_ITEM);
      if (idx == null) {
        return Recognizer.error(new RuntimeException("Inconsistent state"));
      } else {
        this.index = idx;
        this.state = State.Delegated;

        try {
          if (this.builder.feedIndexed(idx, event)) {
            return Recognizer.error(new RuntimeException("Inconsistent state"));
          }
          return this;
        } catch (RuntimeException e) {
          return Recognizer.error(e);
        }
      }
    } else if (event.isStartAttribute()) {
      ReadStartAttribute attribute = (ReadStartAttribute) event;
      Integer idx = this.indexFn.selectIndex(OrdinalFieldKey.attr(attribute.value()));

      if (idx == null) {
        idx = this.indexFn.selectIndex(OrdinalFieldKey.FIRST_ITEM);

        if (idx == null) {
          return Recognizer.error(new RuntimeException(String.format("Unexpected field: %s", attribute.value())));
        } else {
          this.index = idx;
          this.state = State.Delegated;

          try {
            this.builder.feedIndexed(idx, ReadEvent.startAttribute(attribute.value()));
            return this;
          } catch (RuntimeException e) {
            return Recognizer.error(e);
          }
        }
      } else {
        if (!this.bitSet.get(idx)) {
          this.index = idx;
          this.state = State.AttrItem;
          return this;
        } else {
          return Recognizer.error(new RuntimeException(String.format("Duplicate field: %s", attribute.value())));
        }
      }
    } else {
      return Recognizer.error(new RuntimeException("Expected a record body or attribute. Found: " + event));
    }
  }

  protected void transitionFromInit() {
    Integer idx = this.indexFn.selectIndex(OrdinalFieldKey.HEADER);

    if (idx == null) {
      this.state = State.NoHeader;
    } else {
      this.index = idx;
      this.state = State.Header;
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new DelegateClassRecognizer<>(this.tagSpec, this.builder.reset(), this.bitSet.size(), this.indexFn);
  }

  enum State {
    Init,
    Header,
    NoHeader,
    AttrBetween,
    AttrItem,
    Delegated,
  }
}
