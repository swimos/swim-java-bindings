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

import ai.swim.form.annotations.AutoForm;
import ai.swim.form.annotations.FormProperty;
import org.junit.jupiter.api.Test;

class FormTest {

  @Test
  void t() {
    Prop form = new Prop();

  }

  @AutoForm
  @AutoForm.Tag("FormClass")
  static class Prop {
    @AutoForm.Name("Tagged")
    @AutoForm.Property({FormProperty.Skip})
    private String tag;

    @AutoForm.Name("b")
    private int a;
  }

  // should produce

  static class PropForm extends Form<Prop> {

  }

}