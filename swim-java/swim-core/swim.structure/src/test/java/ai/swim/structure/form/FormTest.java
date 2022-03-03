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

import ai.swim.structure.form.annotations.AutoForm;
import ai.swim.structure.form.annotations.FormProperty;
import org.junit.jupiter.api.Test;
import swim.codec.Parser;
import swim.codec.Unicode;
import swim.recon.Recon;
import swim.structure.Value;

class FormTest {

  @Test
  void t() {
    Prop form = new Prop();

  }

  @Test
  void a() {
    Parser<Value> parser = Recon.structureParser().blockParser();
    Parser<Value> feed = parser.feed(Unicode.stringInput("@linked(node:node,lane:lane,rate:0.5,prio:1.0)5"));
    System.out.println(feed.bind());

    //    parser.
//    new String(new byte[]{}, StandardCharsets.UTF_8);

    // @auth{first:1,second:2}
//    byte[] bytes = new byte[]{64, 97, 117, 116, 104, 123, 102, 105, 114, 115, 116, 58, 49, 44, 115, 101, 99, 111, 110, 100, 58, 50, 125};
//    Decoder<Envelope> decoder = Envelope.decoder();
//
//    AuthRequest actual = (AuthRequest) decoder.feed(Binary.input(bytes)).bind();
//    AuthRequest expected = new AuthRequest(Record.of(Slot.of("first", 1), Slot.of("second", 2)));
//    assertEquals(actual, expected);
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

}