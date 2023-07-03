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

import ai.swim.structure.Recon;
import ai.swim.structure.value.*;
import ai.swim.structure.writer.value.ValueStructuralWritable;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconTest {

  void test(Value input, String expectedRecon, String expectedCompact, String expectedPretty) {
    StringWriter stringWriter = new StringWriter();
    Recon.printRecon(stringWriter, new ValueStructuralWritable(), input);
    assertEquals(expectedRecon, stringWriter.toString(), "Recon test failure");

    stringWriter = new StringWriter();
    Recon.printReconCompact(stringWriter, new ValueStructuralWritable(), input);
    assertEquals(expectedCompact, stringWriter.toString(), "Compact test failure");

    stringWriter = new StringWriter();
    Recon.printReconPretty(stringWriter, new ValueStructuralWritable(), input);
    assertEquals(expectedPretty, stringWriter.toString(), "Pretty print failure");
  }

  @Test
  void primitiveValues() {
    test(Value.extant(), "", "", "");
    test(Value.of(1), "1", "1", "1");
    test(Value.of(-10000000000L), "-10000000000", "-10000000000", "-10000000000");
    test(Value.of("text"), "text", "text", "text");
    test(Value.of("two words"), "\"two words\"", "\"two words\"", "\"two words\"");
    test(Value.of(true), "true", "true", "true");
    test(Value.of(0.0), "0.0", "0.0", "0.0");
    test(Value.of(0.0d), "0.0", "0.0", "0.0");
    test(Value.of(BigInteger.TEN), "10", "10", "10");
    test(Value.of(BigDecimal.valueOf(13.14)), "13.14", "13.14", "13.14");
    test(Value.of(new byte[]{1, 2, 3, 4, 5}), "%AQIDBAU=", "%AQIDBAU=", "%AQIDBAU=");
  }

  @Test
  void simpleNoAttributeRecords() {
    test(Value.record(0, 0), "{}", "{}", "{}");

    test(
        Value.ofItems(List.of(Item.valueItem(1))),
        "{ 1 }",
        "{1}",
        "{\n    1\n}"
    );

    test(
        Value.ofItems(List.of(Value.ofItem(Value.of("name"), Value.of(1)))),
        "{ name: 1 }",
        "{name:1}",
        "{\n    name: 1\n}"
    );

    test(
        Value.ofItems(List.of(Item.valueItem(1), Item.valueItem(2), Item.valueItem(3))),
        "{ 1, 2, 3 }",
        "{1,2,3}",
        "{\n    1,\n    2,\n    3\n}"
    );

    test(Value.ofItems(List.of(
                Value.ofItem(Value.of("first"), Value.of(1)),
                Value.ofItem(Value.of("second"), Value.of(2)),
                Value.ofItem(Value.of("third"), Value.of(3))
            )
        ),
        "{ first: 1, second: 2, third: 3 }",
        "{first:1,second:2,third:3}",
        "{\n    first: 1,\n    second: 2,\n    third: 3\n}"
    );
  }

  @Test
  void simpleAttributes() {
    test(
        Value.ofAttrs(List.of(Value.ofAttr("tag"))),
        "@tag",
        "@tag",
        "@tag"
    );

    test(
        Value.ofAttrs(List.of(Value.ofAttr("tag", Value.of(1)))),
        "@tag(1)",
        "@tag(1)",
        "@tag(1)"
    );

    test(
        Value.ofAttrs(List.of(Value.ofAttr("tag", Value.record(0, 0)))),
        "@tag({})",
        "@tag({})",
        "@tag({})"
    );

    test(
        Value.ofAttrs(List.of(Value.ofAttr("tag", Value.ofItems(List.of(Item.valueItem(1)))))),
        "@tag({ 1 })",
        "@tag({1})",
        "@tag({\n    1\n})"
    );

    test(
        Value.ofAttrs(
            List.of(Value.ofAttr("tag",
                Value.ofItems(List.of(Value.ofItem(Text.of("name"), Value.of(1)))))
            )
        ),
        "@tag(name: 1)",
        "@tag(name:1)",
        "@tag(name: 1)"
    );

    test(
        Value.ofAttrs(
            List.of(Value.ofAttr("tag",
                Value.ofItems(
                    List.of(Item.valueItem(1), Item.valueItem(2), Item.valueItem(3))
                ))
            )
        ),
        "@tag(1, 2, 3)",
        "@tag(1,2,3)",
        "@tag(1, 2, 3)"
    );

    test(
        Value.ofAttrs(
            List.of(Value.ofAttr("tag",
                Value.ofItems(
                    List.of(Item.valueItem(1), Value.ofItem(Value.of("slot"), Value.of(2)), Item.valueItem(3))
                ))
            )
        ),
        "@tag(1, slot: 2, 3)",
        "@tag(1,slot:2,3)",
        "@tag(1, slot: 2, 3)"
    );
  }

  @Test
  void nestedNoAttributeRecords() {
    Value inner = Value.ofItems(
        List.of(Item.valueItem(1), Item.valueItem(2), Item.valueItem(3))
    );

    test(
        Value.ofItems(List.of(Value.ofItem(inner))),
        "{ { 1, 2, 3 } }",
        "{{1,2,3}}",
        "{\n    {\n        1,\n        2,\n        3\n    }\n}"
    );

    test(
        Value.ofItems(List.of(Value.ofItem(Text.of("name"), inner))),
        "{ name: { 1, 2, 3 } }",
        "{name:{1,2,3}}",
        "{\n    name: {\n        1,\n        2,\n        3\n    }\n}"
    );

    test(
        Value.ofItems(List.of(Item.valueItem(1), Item.valueItem(2), Value.ofItem(inner))),
        "{ 1, 2, { 1, 2, 3 } }",
        "{1,2,{1,2,3}}",
        "{\n    1,\n    2,\n    {\n        1,\n        2,\n        3\n    }\n}"
    );
  }

  @Test
  void completeRecords() {
    Attr first = Value.ofAttr("first");
    Attr second = Value.ofAttr("second", Value.of(1));
    List<Item> items = List.of(Item.valueItem(1), Value.ofItem(Value.of("name"), Value.of(2)), Item.valueItem(true));

    test(
        Value.of(List.of(first), items),
        "@first { 1, name: 2, true }",
        "@first{1,name:2,true}",
        "@first {\n    1,\n    name: 2,\n    true\n}"
    );

    test(
        Value.of(List.of(first), List.of(Item.valueItem(1))),
        "@first 1",
        "@first 1",
        "@first 1"
    );

    test(
        Value.of(List.of(first, second), items),
        "@first @second(1) { 1, name: 2, true }",
        "@first@second(1){1,name:2,true}",
        "@first @second(1) {\n    1,\n    name: 2,\n    true\n}"
    );
  }

  @Test
  void complexAttributes() {
    Attr first = Value.ofAttr("first");
    Attr second = Value.ofAttr("second", Value.of(1));
    List<Item> items = List.of(Item.valueItem(1), Value.ofItem(Value.of("name"), Value.of(2)), Item.valueItem(true));
    Record record = Value.ofAttrs(
        List.of(Value.ofAttr("tag", Value.of(
                List.of(first),
                items
            )
        ))
    );


    test(
        record,
        "@tag(@first { 1, name: 2, true })",
        "@tag(@first{1,name:2,true})",
        "@tag(@first {\n    1,\n    name: 2,\n    true\n})"
    );

    test(
        Value.ofAttrs(
            List.of(Value.ofAttr("tag", Value.of(
                List.of(first),
                List.of(Item.valueItem(1))
            )))),
        "@tag(@first 1)",
        "@tag(@first 1)",
        "@tag(@first 1)"
    );

    test(
        Value.ofAttrs(
            List.of(Value.ofAttr("tag", Value.of(
                List.of(first, second),
                items
            )))),
        "@tag(@first @second(1) { 1, name: 2, true })",
        "@tag(@first@second(1){1,name:2,true})",
        "@tag(@first @second(1) {\n    1,\n    name: 2,\n    true\n})"
    );

    test(
        Value.ofAttrs(
            List.of(Value.ofAttr("tag", Value.of(
                Collections.emptyList(),
                List.of(
                    Item.valueItem(1),
                    Value.ofItem(Value.of(
                        List.of(first, second),
                        items
                    )),
                    Value.ofItem(Text.of("slot"), Value.of(2)),
                    Item.valueItem(3)
                )
            )))
        ),
        "@tag(1, @first @second(1) { 1, name: 2, true }, slot: 2, 3)",
        "@tag(1,@first@second(1){1,name:2,true},slot:2,3)",
        "@tag(1, @first @second(1) {\n    1,\n    name: 2,\n    true\n}, slot: 2, 3)"
    );
  }

}