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
import ai.swim.structure.processor.recognizer.StructuralRecognizer;
import ai.swim.structure.processor.schema.Schema;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;

@AutoService(Processor.class)
public class FormProcessor extends AbstractProcessor {

  private final List<Schema> schemas = new ArrayList<>();
  private ProcessingContext processingContext;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(AutoForm.class)) {
      if (!element.getKind().isClass() && !element.getKind().isInterface()) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, element + " cannot be annotated with @" + AutoForm.class.getSimpleName() + ". It may only be used on classes and interfaces");
        return true;
      }

      if (!element.asType().toString().contains("ClassB")) {
//        continue;
      }

      ScopedContext scopedContext = this.processingContext.enter(element);

      try {
        // Anything that we're processing will be structural
        StructuralRecognizer recognizer = (StructuralRecognizer) scopedContext.getRecognizer(element);

        if (recognizer == null) {
          return true;
        }

        this.schemas.add(Schema.from(recognizer));
      } catch (Throwable e) {
        processingContext.getProcessingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
        throw e;
      }
    }

    if (roundEnv.processingOver()) {
      // The files written out by this method call must **not** generate any classes that use the @AutoForm annotation
      // as they will **not** be subject to any further annotation processing as this is now the final round.
      //
      // As we are generating files on the final round, this causes the compiler to emit a warning message, but it is
      // safe to ignore.
      write();
    }

    return true;
  }

  private void write() {
    for (Schema schema : this.schemas) {
      try {
        schema.write(this.processingContext.enter(schema.root()));
      } catch (Throwable e) {
        e.printStackTrace();
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
        return;
      }
    }
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.processingContext = new ProcessingContext(processingEnv);
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
