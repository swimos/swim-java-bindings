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

package ai.swim.structure.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

public class TestProcessor {

  private static Compilation compile(String resource) {
    return javac()
        .withProcessors(new FormProcessor())
        .compile(JavaFileObjects.forResource(resource));
  }

  @Test
  void badTag() {
    assertThat(compile("BadTag.java"))
        .hadErrorContaining("invalid characters in tag: 'badtag!!'");
  }

  @Test
  void tagOnEnumRoot() {
    assertThat(compile("TagOnEnumRoot.java"))
        .hadErrorContaining(
            "ai.swim.structure.annotations.AutoForm.Tag cannot be used on enumerations, only on constants");
  }

  @Test
  void duplicateTag() {
    assertThat(compile("DuplicateTag.java"))
        .hadErrorContaining("contains a duplicate tag: 'A'");
  }

  @Test
  void duplicateTag2() {
    assertThat(compile("DuplicateTag2.java"))
        .hadErrorContaining("contains a duplicate tag: 'A'");
  }

  @Test
  void noSetter() {
    assertThat(compile("NoSetter.java"))
        .hadErrorContaining("Private field: 'a' has no setter");
  }

  @Test
  void noGetter() {
    assertThat(compile("NoGetter.java"))
        .hadErrorContaining("Private field: 'a' has no getter");
  }

  @Test
  void badSetter() {
    assertThat(compile("BadSetter.java"))
        .hadErrorContaining(
            "setter for field 'a' accepts an incorrect type. Cause: Expected type: 'float', found: 'int'");
  }

  @Test
  void badSetter2() {
    assertThat(compile("BadSetter2.java"))
        .hadErrorContaining("expected a setter for field 'a' that takes one parameter");
  }

  @Test
  void badGetter() {
    assertThat(compile("BadGetter.java"))
        .hadErrorContaining("getter for field 'a' returns an incorrect type");
  }

  @Test
  void badGetter2() {
    assertThat(compile("BadGetter2.java"))
        .hadErrorContaining("getter for field 'a' should have no parameter");
  }

  @Test
  void noConstructor() {
    assertThat(compile("NoConstructor.java"))
        .hadErrorContaining("Class must contain a public constructor with no arguments");
  }

  @Test
  void badSubType() {
    assertThat(compile("BadSubType.java"))
        .hadErrorContaining("BadSubType.Child is not a subtype of BadSubType.Parent");
  }

  @Test
  void badSubType2() {
    assertThat(compile("BadSubType2.java"))
        .hadErrorContaining("Class missing subtypes");
  }

  @Test
  void badSubType3() {
    assertThat(compile("BadSubType3.java"))
        .hadErrorContaining("Subtype cannot be root type");
  }

  @Test
  void badSubType4() {
    assertThat(compile("BadSubType4.java"))
        .hadErrorContaining(
            "Class extends from 'BadSubType4.Parent' that is not annotated with @AutoForm. Either annotate it or manually implement a form");
  }

  @Test
  void duplicateField() {
    assertThat(compile("DuplicateField.java"))
        .hadErrorContaining("Class contains a field (field) with the same name as one in its superclass");
  }

  @Test
  void duplicateField2() {
    assertThat(compile("DuplicateField2.java"))
        .hadErrorContaining("Class contains a field (fieldA) with the same name as one in its superclass");
  }

  @Test
  void doubleBody() {
    assertThat(compile("DoubleBody.java"))
        .hadErrorContaining("At most one field can replace the body");
  }

  @Test
  void doubleHeaderBody() {
    assertThat(compile("DoubleHeaderBody.java"))
        .hadErrorContaining("At most one field can replace the tag body.");
  }

  @Test
  void recursiveType() {
    assertThat(compile("RecursiveType.java"))
        .hadErrorContaining("Recursive types are not supported by the form annotation processor.");
  }

}
