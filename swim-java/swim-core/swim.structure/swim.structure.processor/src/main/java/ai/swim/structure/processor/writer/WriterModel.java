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

package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.ClassMap;
import ai.swim.structure.processor.models.InterfaceMap;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.RuntimeLookupModel;
import ai.swim.structure.processor.schema.ClassSchema;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.Utils.isSubType;
import static ai.swim.structure.processor.writer.Lookups.LIST_WRITER_CLASS;
import static ai.swim.structure.processor.writer.Lookups.MAP_WRITER_CLASS;

public class WriterModel extends Model {
  public static final String RUNTIME_LOOKUP = "WriterTypeParameter";

  protected WriterModel(TypeMirror type) {
    super(type);
  }

  public static void write(Model model, ScopedContext context) throws IOException {
    if (model.isClass()) {
      ClassWriter.writeClass(ClassSchema.fromMap((ClassMap) model), context);
    } else if (model.isInterface()) {
      InterfaceMap interfaceMap = (InterfaceMap) model;
    } else {
      throw new RuntimeException("Unimplemented schema type: " + model);
    }
  }

  public static Model from(TypeMirror typeMirror, ScopedContext context) {
    Model writer = context.getWriterFactory().lookup(typeMirror);

    if (writer != null) {
      return writer;
    }

    writer = WriterModel.fromStdType(typeMirror, context);

    if (writer != null) {
      return writer;
    }

    switch (typeMirror.getKind()) {
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) typeMirror;

        if (declaredType.getTypeArguments().isEmpty()) {
          return new RuntimeLookupModel(RUNTIME_LOOKUP, typeMirror, null);
        } else {
          Model[] typeParameters = declaredType.getTypeArguments().stream().map(ty -> WriterModel.from(ty, context)).collect(Collectors.toList()).toArray(Model[]::new);
          return new RuntimeLookupModel(RUNTIME_LOOKUP, typeMirror, typeParameters);
        }
      case TYPEVAR:
        return new ReflectModel(typeMirror);
      case WILDCARD:
        throw new AssertionError("Writer model wildcard type");
      default:
        // We're out of options now. The writer isn't available to us now, so we'll have to hope that it's been
        // registered with the writer proxy for a runtime lookup.
        return new RuntimeLookupModel(RUNTIME_LOOKUP, typeMirror, null);
    }
  }

  private static Model fromStdType(TypeMirror mirror, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    if (isSubType(processingEnvironment, mirror, Collection.class)) {
      return Model.singleGeneric(
          new WriterModelLookup(),
          elementUtils.getTypeElement(List.class.getCanonicalName()),
          elementUtils.getTypeElement(LIST_WRITER_CLASS),
          mirror,
          context
      );
    }

    if (isSubType(processingEnvironment, mirror, Map.class)) {
      return Model.twoGenerics(
          new WriterModelLookup(),
          elementUtils.getTypeElement(Map.class.getCanonicalName()),
          elementUtils.getTypeElement(MAP_WRITER_CLASS),
          mirror,
          context
      );
    }

    return null;
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor, boolean isAbstract) {
    return null;
  }
}
