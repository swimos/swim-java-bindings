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

package ai.swim.structure.processor.writer.writerForm;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.InterfaceModel;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.Writer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
import java.io.IOException;
import static ai.swim.structure.processor.writer.writerForm.Lookups.WRITER_PROXY;

public class WriterFormWriter implements Writer {
  private final ProcessingEnvironment environment;

  public WriterFormWriter(ProcessingEnvironment environment) {
    this.environment = environment;
  }

  @Override
  public void writeClass(ClassLikeModel model) throws IOException {
    WriterContext context = WriterContext.build(
        model.getElement(),
        environment,
        model.getJavaClassName(),
        model.getDeclaredPackage());
    TypeSpec typeSpec;

    if (model.isAbstract()) {
      typeSpec = new AbstractClassWriter(model.getElement(), context, model.getSubTypes()).build();
    } else {
      typeSpec = new ConcreteClassWriter(
          model.getElement(),
          context,
          model,
          PartitionedFields.buildFrom(model.getFields())).build();
    }

    writeTypeSpec(typeSpec, model.getDeclaredPackage());
  }

  @Override
  public void writeInterface(InterfaceModel model) throws IOException {
    WriterContext context = WriterContext.build(
        model.getElement(),
        environment,
        model.getJavaClassName(),
        model.getDeclaredPackage());
    TypeSpec typeSpec = new AbstractClassWriter(model.getElement(), context, model.getSubTypes()).build();

    writeTypeSpec(typeSpec, model.getDeclaredPackage());
  }

  private void writeTypeSpec(TypeSpec typeSpec, PackageElement packageElement) throws IOException {
    JavaFile javaFile = JavaFile.builder(packageElement.toString(), typeSpec)
        .addStaticImport(ClassName.bestGuess(WRITER_PROXY), "getProxy")
        .build();

    javaFile.writeTo(environment.getFiler());
  }
}
