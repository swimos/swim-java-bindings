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

package ai.swim.structure.form;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.ClassRecognizerInit;
import ai.swim.structure.form.recognizer.structural.key.ItemFieldKey;
import ai.swim.structure.form.recognizer.structural.tag.FixedTagSpec;
import java.util.Objects;

public class InnerPropClass {

  private final int a;
  private final int b;

  public InnerPropClass(int a, int b) {
    this.a = a;
    this.b = b;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InnerPropClass that = (InnerPropClass) o;
    return a == that.a && b == that.b;
  }

  @Override
  public int hashCode() {
    return Objects.hash(a, b);
  }

  @Override
  public String toString() {
    return "InnerPropClass{" + "a=" + a + ", b=" + b + '}';
  }

  @AutoloadedRecognizer(InnerPropClass.class)
  public static class InnerClassRecognizer extends Recognizer<InnerPropClass> {

    private Recognizer<InnerPropClass> recognizer;

    public InnerClassRecognizer() {
      this.recognizer = new ClassRecognizerInit<>(new FixedTagSpec(InnerPropClass.class.getSimpleName()), new InnerPropClassBuilder(), 2, (key) -> {
        if (key.isItem()) {
          ItemFieldKey itemFieldKey = (ItemFieldKey) key;
          switch (itemFieldKey.getName()) {
            case "a":
              return 0;
            case "b":
              return 1;
            default:
              throw new RuntimeException("Unexpected key: " + key);
          }
        }
        return null;
      });
    }

    @Override
    public Recognizer<InnerPropClass> feedEvent(ReadEvent event) {
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
    public InnerPropClass bind() {
      return this.recognizer.bind();
    }

    @Override
    public RuntimeException trap() {
      return this.recognizer.trap();
    }

  }

  public static class InnerPropClassBuilder implements RecognizingBuilder<InnerPropClass> {

    private final ClassRecognizerTest.FieldRecognizingBuilder<Integer> aBuilder = new ClassRecognizerTest.FieldRecognizingBuilder<>(Integer.class);
    private final ClassRecognizerTest.FieldRecognizingBuilder<Integer> bBuilder = new ClassRecognizerTest.FieldRecognizingBuilder<>(Integer.class);

    @Override
    public boolean feedIndexed(int index, ReadEvent event) {
      switch (index) {
        case 0:
          return this.aBuilder.feed(event);
        case 1:
          return this.bBuilder.feed(event);
        default:
          throw new RuntimeException("Unknown idx: " + index);
      }
    }

    @Override
    public InnerPropClass bind() {
      return new InnerPropClass(this.aBuilder.bind(), this.bBuilder.bind());
    }


  }


}
