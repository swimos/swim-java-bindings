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

package ai.swim.structure.processor.writer;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.TypeParameterElement;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class WriterUtils {

  public static void writeIndexSwitchBlock(CodeBlock.Builder body,
      String switchOn,
      int startAt,
      BiFunction<Integer, Integer, String> caseWriter) {
    body.beginControlFlow("switch ($L)", switchOn);

    int i = startAt;

    while (true) {
      String caseStatement = caseWriter.apply(startAt, i);
      if (caseStatement == null) {
        break;
      } else {
        body.add(caseStatement);
        i += 1;
      }
    }

    body.add("default:");
    body.addStatement("\tthrow new RuntimeException(\"Unexpected key: \" + key)");
    body.endControlFlow();
  }

  public static List<TypeVariableName> typeParametersToTypeVariable(Collection<? extends TypeParameterElement> typeParameters) {
    return typeParameters.stream().map(tp -> {
      TypeName[] bounds = tp
          .getBounds()
          .stream()
          .map(TypeName::get)
          .collect(Collectors.toList())
          .toArray(new TypeName[] {});
      return TypeVariableName.get(tp.asType().toString(), bounds);
    }).collect(Collectors.toList());
  }


}
