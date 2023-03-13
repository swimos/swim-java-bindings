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

package ai.swim.structure.processor.recognizer.writer.builder;

import ai.swim.structure.processor.Emitter;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

import static ai.swim.structure.processor.recognizer.writer.Lookups.RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.recognizer.writer.Lookups.RECOGNIZING_BUILDER_FEED_INDEX;
import static ai.swim.structure.processor.recognizer.writer.Lookups.RECOGNIZING_BUILDER_RESET;
import static ai.swim.structure.processor.recognizer.writer.Lookups.TYPE_READ_EVENT;

public abstract class Builder {

  protected final ClassSchema schema;
  protected final ScopedContext context;
  protected TypeName target;
  protected List<FieldModel> fields;

  public Builder(ClassSchema schema, ScopedContext context) {
    this.schema = schema;
    this.context = context;
  }

  public TypeSpec build(TypeName ty) {
    this.target = ty;
    this.fields = getFields();

    TypeSpec.Builder builder = init();

    builder.addSuperinterface(ty);
    builder.addFields(buildFields());
    builder.addMethods(buildMethods());

    return builder.build();
  }

  protected List<FieldSpec> buildFields() {
    List<FieldSpec> fieldSpecs = new ArrayList<>(fields.size());

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    for (FieldModel recognizer : this.fields) {
      TypeElement fieldFieldRecognizingBuilder = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
      TypeMirror recognizerType = recognizer.boxedType(processingEnvironment);

      DeclaredType memberRecognizingBuilder = typeUtils.getDeclaredType(fieldFieldRecognizingBuilder, recognizerType);
      FieldSpec.Builder fieldSpec = FieldSpec.builder(TypeName.get(memberRecognizingBuilder), context.getNameFactory().fieldBuilderName(recognizer.getName().toString()), Modifier.PRIVATE);

      fieldSpecs.add(fieldSpec.build());
    }

    return fieldSpecs;
  }

  private List<MethodSpec> buildMethods() {
    List<MethodSpec> methods = buildConstructors();
    methods.addAll(List.of(buildFeedIndexed(), buildBind(), buildReset()));
    return methods;
  }

  protected abstract List<MethodSpec> buildConstructors();

  private MethodSpec buildReset() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_RESET)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(this.target);
    builder.addCode(buildResetBlock().emit(context));

    return builder.build();
  }

  protected abstract MethodSpec buildBind();

  private MethodSpec buildFeedIndexed() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    TypeElement readEventType = elementUtils.getTypeElement(TYPE_READ_EVENT);
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_FEED_INDEX)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Integer.TYPE, "index")
        .addParameter(TypeName.get(readEventType.asType()), "event")
        .returns(boolean.class);

    builder.addCode(buildFeedIndexedBlock().emit(context));

    return builder.build();
  }

  protected abstract TypeSpec.Builder init();

  protected abstract Emitter buildFeedIndexedBlock();

  protected abstract Emitter buildResetBlock();

  protected abstract List<FieldModel> getFields();
}
