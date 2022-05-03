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

package ai.swim.structure.processor;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.context.ProcessingContext;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.structure.ClassSchema;
import ai.swim.structure.processor.writer.RecognizerWriter;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

@AutoService(Processor.class)
public class FormProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    ProcessingContext processingContext = new ProcessingContext(this.processingEnv);
    List<String> classNames = new ArrayList<>();

    for (Element element : roundEnv.getElementsAnnotatedWith(AutoForm.class)) {
      if (element.getKind() != ElementKind.CLASS) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, element + " cannot be annotated with @" + AutoForm.class.getSimpleName() + ". It may only be used on classes");
        return true;
      }

      ElementMap elementMap = processingContext.getMap(element);
      if (elementMap == null) {
        return true;
      }

      ScopedContext scopedContext = new ScopedContext(processingContext, element);
      ClassSchema classSchema = ClassSchema.fromMap(scopedContext, elementMap);
      if (classSchema == null) {
        return true;
      }

      classNames.add(classSchema.className());

      try {
        RecognizerWriter.writeRecognizer(classSchema, scopedContext);
      } catch (IOException e) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
        return true;
      }
    }

    return true;
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotations = new LinkedHashSet<>();
    annotations.add(AutoForm.class.getCanonicalName());

    return Collections.unmodifiableSet(annotations);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

}
