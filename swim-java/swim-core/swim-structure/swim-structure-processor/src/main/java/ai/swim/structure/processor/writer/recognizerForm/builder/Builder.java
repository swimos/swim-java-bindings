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

package ai.swim.structure.processor.writer.recognizerForm.builder;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZING_BUILDER_FEED_INDEX;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZING_BUILDER_RESET;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.TYPE_READ_EVENT;

/**
 * Recognizer builder shared functionality for use between class and header builders.
 */
public abstract class Builder {
  /**
   * The model that is being built.
   */
  protected final ClassLikeModel model;
  /**
   * Discriminated Recon fields.
   */
  protected final PartitionedFields partitionedFields;
  /**
   * Recognizer context scoped to the root processing element.
   */
  protected final RecognizerContext context;
  /**
   * The name of the target class.
   */
  protected TypeName target;
  /**
   * The fields that will be used by the recognizer.
   */
  protected List<FieldModel> fields;

  public Builder(ClassLikeModel model, PartitionedFields partitionedFields, RecognizerContext context) {
    this.model = model;
    this.partitionedFields = partitionedFields;
    this.context = context;
  }

  /**
   * Builds and returns a {@code TypeSpec} for this Recognizer.
   *
   * @param ty the name of the class.
   * @return an initialized type spec for this recognizer.
   */
  public TypeSpec build(TypeName ty) {
    this.target = ty;
    this.fields = getFields();

    TypeSpec.Builder builder = init();

    builder.addSuperinterface(ty);
    builder.addFields(buildFields());
    builder.addMethods(buildMethods());

    return builder.build();
  }

  /**
   * Transpose the fields into {@link FieldSpec}s.
   */
  protected List<FieldSpec> buildFields() {
    List<FieldSpec> fieldSpecs = new ArrayList<>(fields.size());
    Elements elementUtils = context.getElementUtils();
    Types typeUtils = context.getTypeUtils();

    for (FieldModel recognizer : this.fields) {
      TypeElement fieldFieldRecognizingBuilder = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
      TypeMirror recognizerType = recognizer.boxedType(context.getProcessingEnvironment());

      DeclaredType memberRecognizingBuilder = typeUtils.getDeclaredType(fieldFieldRecognizingBuilder, recognizerType);
      FieldSpec.Builder fieldSpec = FieldSpec.builder(
          TypeName.get(memberRecognizingBuilder),
          context
              .getFormatter()
              .fieldBuilderName(recognizer.getName().toString()),
          Modifier.PRIVATE);

      fieldSpecs.add(fieldSpec.build());
    }

    return fieldSpecs;
  }

  /**
   * Transpose the methods into {@link MethodSpec}s.
   */
  private List<MethodSpec> buildMethods() {
    List<MethodSpec> methods = buildConstructors();
    methods.addAll(List.of(buildFeedIndexed(), buildBind(), buildReset()));
    return methods;
  }

  /**
   * Build the constructors required for this builder.
   */
  protected abstract List<MethodSpec> buildConstructors();

  /**
   * Build the recognizer's reset method.
   */
  private MethodSpec buildReset() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_RESET)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(this.target);
    builder.addCode(buildResetBlock().toString());

    return builder.build();
  }

  /**
   * Build the recognizer's bind method.
   */
  protected abstract MethodSpec buildBind();

  /**
   * Build the recognizer's reset method.
   */
  private MethodSpec buildFeedIndexed() {
    Elements elementUtils = context.getElementUtils();
    TypeElement readEventType = elementUtils.getTypeElement(TYPE_READ_EVENT);
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_FEED_INDEX)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Integer.TYPE, "index")
        .addParameter(TypeName.get(readEventType.asType()), "event")
        .returns(boolean.class);

    builder.addCode(buildFeedIndexedBlock().toString());

    return builder.build();
  }

  /**
   * Initialize the builder.
   */
  protected abstract TypeSpec.Builder init();

  /**
   * Build the recognizer's feed index method body.
   */
  protected abstract Emitter buildFeedIndexedBlock();

  /**
   * Build the recognizer's reset method body.
   */
  protected abstract Emitter buildResetBlock();

  /**
   * Returns the fields that this builder expects.
   */
  protected abstract List<FieldModel> getFields();
}
