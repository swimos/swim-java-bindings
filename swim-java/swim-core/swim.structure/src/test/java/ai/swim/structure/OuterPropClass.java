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

package ai.swim.structure;


import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.ClassRecognizerInit;
import ai.swim.structure.recognizer.structural.key.ItemFieldKey;
import ai.swim.structure.recognizer.structural.tag.FixedTagSpec;

import java.util.Objects;

class OuterPropClass {

  private final String c;
  private final InnerPropClass d;

  public OuterPropClass(String c, InnerPropClass d) {
    this.c = c;
    this.d = d;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OuterPropClass that = (OuterPropClass) o;
    return Objects.equals(c, that.c) && Objects.equals(d, that.d);
  }

  @Override
  public int hashCode() {
    return Objects.hash(c, d);
  }

  @Override
  public String toString() {
    return "OuterPropClass{" +
        "c='" + c + '\'' +
        ", d=" + d +
        '}';
  }


  static class OuterPropClassBuilder implements RecognizingBuilder<OuterPropClass> {

    private final FieldRecognizingBuilder<String> cBuilder = new FieldRecognizingBuilder<>(String.class);
    private final FieldRecognizingBuilder<InnerPropClass> dBuilder = new FieldRecognizingBuilder<>(new InnerPropClass.InnerClassRecognizer());

    @Override
    public boolean feedIndexed(int index, ReadEvent event) {
      switch (index) {
        case 0:
          return this.cBuilder.feed(event);
        case 1:
          return this.dBuilder.feed(event);
        default:
          throw new RuntimeException("Unknown idx: " + index);
      }
    }

    @Override
    public OuterPropClass bind() {
      return new OuterPropClass(this.cBuilder.bind(), this.dBuilder.bind());
    }

  }

  @AutoloadedRecognizer(OuterPropClass.class)
  public static class OuterClassRecognizer extends Recognizer<OuterPropClass> {

    public Recognizer<OuterPropClass> recognizer;

    public OuterClassRecognizer() {
      this.recognizer = new ClassRecognizerInit<>(new FixedTagSpec(OuterPropClass.class.getSimpleName()), new OuterPropClassBuilder(), 2, (key) -> {
        if (key.isItem()) {
          ItemFieldKey itemFieldKey = (ItemFieldKey) key;
          switch (itemFieldKey.getName()) {
            case "c":
              return 0;
            case "d":
              return 1;
            default:
              throw new RuntimeException("Unexpected key: " + key);
          }
        }
        return null;
      });
    }

    @Override
    public Recognizer<OuterPropClass> feedEvent(ReadEvent event) {
      this.recognizer = this.recognizer.feedEvent(event);
      return this;
    }

    @Override
    public boolean isCont() {
      return this.recognizer.isCont();
    }

    @Override
    public boolean isDone() {
      return this.recognizer.isDone();
    }

    @Override
    public boolean isError() {
      return this.recognizer.isError();
    }

    @Override
    public OuterPropClass bind() {
      return this.recognizer.bind();
    }

    @Override
    public RuntimeException trap() {
      return this.recognizer.trap();
    }

    @Override
    public Recognizer<OuterPropClass> reset() {
      return null;
    }

  }

}