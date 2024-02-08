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

package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.model.FieldModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Description of how the fields of a type are written into a record.
 */
public class PartitionedFields {
  public final HeaderSpec headerSpec;
  public final Body body;

  public PartitionedFields(HeaderSpec headerSpec, Body body) {
    this.headerSpec = headerSpec;
    this.body = body;
  }

  /**
   * Partitions {@code fields} according to their field kind.
   */
  public static PartitionedFields buildFrom(List<FieldModel> fields) {
    HeaderSpec headerSpec = new HeaderSpec();
    Body body = new Body();

    for (FieldModel field : fields) {
      switch (field.getFieldKind()) {
        case Body:
          List<FieldModel> newHeaderFields = body.replace(field);
          headerSpec.addHeaderFields(newHeaderFields);
          break;
        case Header:
          headerSpec.addHeaderField(field);
          break;
        case HeaderBody:
          if (!headerSpec.hasTagBody()) {
            headerSpec.setTagBody(field);
          }
          break;
        case Attr:
          headerSpec.addAttribute(field);
          break;
        case Slot:
          if (!body.isReplaced()) {
            body.addField(field);
          } else {
            headerSpec.addHeaderField(field);
          }
          break;
      }
    }

    return new PartitionedFields(headerSpec, body);
  }

  /**
   * Returns the total number of header and body fields.
   */
  public int count() {
    return headerSpec.count() + body.count();
  }

  /**
   * Returns whether there are any header fields.
   */
  public boolean hasHeaderFields() {
    return !this.headerSpec.headerFields.isEmpty() || this.headerSpec.hasTagBody();
  }

  /**
   * Discriminates against all of the fields by their type; tag, header, attribute, item or body.
   *
   * @return
   */
  public List<FieldDiscriminant> discriminate() {
    List<FieldDiscriminant> discriminates = new ArrayList<>();

    FieldModel tagName = this.headerSpec.tagName;
    if (tagName != null) {
      discriminates.add(FieldDiscriminant.tag(tagName));
    }

    if (headerSpec.hasTagBody() || !headerSpec.headerFields.isEmpty()) {
      discriminates.add(FieldDiscriminant.header(headerSpec.tagBody, headerSpec.headerFields));
    }

    discriminates.addAll(headerSpec.attributes.stream().map(FieldDiscriminant::attribute).collect(Collectors.toList()));

    List<FieldModel> bodyFields = body.getFields();

    if (body.isReplaced()) {
      discriminates.add(FieldDiscriminant.body(bodyFields.get(0)));
    } else {
      discriminates.addAll(bodyFields.stream().map(FieldDiscriminant::item).collect(Collectors.toList()));
    }

    return discriminates;
  }

  @Override
  public String toString() {
    return "PartitionedFields{" +
        "headerFields=" + headerSpec +
        ", body=" + body +
        '}';
  }

  /**
   * Returns whether {@code model} is a header.
   */
  public boolean isHeader(FieldModel model) {
    return headerSpec.isHeader(model);
  }

}
