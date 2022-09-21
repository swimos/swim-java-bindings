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

package ai.swim.structure.processor.recognizer.writer.builder.header;

import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.writer.Emitter;
import ai.swim.structure.processor.recognizer.writer.Lookups;
import ai.swim.structure.processor.recognizer.writer.builder.Builder;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HeaderBuilder extends Builder {
  public HeaderBuilder(ClassSchema classSchema, ScopedContext context) {
    super(classSchema, context);
  }

  @Override
  protected TypeSpec.Builder init() {
    return TypeSpec.classBuilder(context.getNameFactory().headerBuilderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
  }

  @Override
  protected MethodSpec buildBind() {
    ClassName classType = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());
    MethodSpec.Builder builder = MethodSpec.methodBuilder(Lookups.RECOGNIZING_BUILDER_BIND)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(classType);
    builder.addCode(buildBindBlock().emit(context));

    return builder.build();
  }

  @Override
  protected Emitter buildBindBlock() {
    return new BindEmitter(fields);
  }

  @Override
  protected Emitter buildFeedIndexedBlock() {
    return new FeedIndexedEmitther(fields);
  }

  @Override
  protected Emitter buildResetBlock() {
    return new ResetEmitter(fields);
  }

  @Override
  protected List<FieldModel> getFields() {
    return schema.discriminate()
        .stream()
        .filter(FieldDiscriminate::isHeader)
        .flatMap(f -> {
          FieldDiscriminate.HeaderFields headerFields = (FieldDiscriminate.HeaderFields) f;
          FieldModel tagBody = headerFields.getTagBody();

          if (tagBody != null) {
            List<FieldModel> fields = new ArrayList<>(Collections.singleton(headerFields.getTagBody()));
            fields.addAll(headerFields.getFields());
            return fields.stream();
          } else {
            return headerFields.getFields().stream();
          }
        })
        .collect(Collectors.toList());
  }
}
