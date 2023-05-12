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

package ai.swim.structure.processor.writer.recognizerForm.builder.header;

import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerNameFormatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recognizer header builder bind method emitter.
 * <p>
 * Builds a bind block like:
 * <pre>
 * {@code
 *     @Override
 *     public OptionalFieldClass bind() {
 *       OptionalFieldClass obj = new OptionalFieldClass();
 *
 *       obj.b = this.bBuilder.bind();
 *       obj.setA(this.aBuilder.bindOr(0));
 *
 *       return obj;
 *     }
 * }
 * </pre>
 */
public class BindEmitter extends Emitter {
  private final List<FieldModel> fields;
  private final RecognizerContext context;

  public BindEmitter(List<FieldModel> fields, RecognizerContext context) {
    this.fields = fields;
    this.context = context;
  }


  @Override
  public String toString() {
    RecognizerNameFormatter formatter = context.getFormatter();
    LinkedHashSet<TypeName> typeParameters = new LinkedHashSet<>();
    for (FieldModel field : fields) {
      typeParameters.addAll(field.typeParameters().stream().map(TypeName::get).collect(Collectors.toList()));
    }

    TypeName classType;
    if (typeParameters.isEmpty()) {
      classType = ClassName.bestGuess(formatter.headerCanonicalName());
    } else {
      classType = ParameterizedTypeName.get(ClassName.bestGuess(formatter.headerCanonicalName()), typeParameters.toArray(TypeName[]::new));
    }

    CodeBlock.Builder body = CodeBlock.builder();
    body.add("$T obj = new $T();\n\n", classType, classType);

    for (FieldModel field : fields) {
      String builderName = formatter.fieldBuilderName(field.getName().toString());

      if (field.isOptional()) {
        body.addStatement("obj.$L = this.$L.bindOr($L)", field.getName().toString(), builderName, field.defaultValue());
      } else {
        body.addStatement("obj.$L = this.$L.bind()", field.getName().toString(), builderName);
      }
    }

    body.add("\nreturn obj;");

    return body.build().toString();
  }
}
