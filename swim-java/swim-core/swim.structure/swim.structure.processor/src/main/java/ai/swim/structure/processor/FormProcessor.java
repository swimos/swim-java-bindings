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
import ai.swim.structure.processor.recognizer.context.ProcessingContext;
import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.models.RecognizerFactory;
import ai.swim.structure.processor.recognizer.models.RecognizerModel;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@AutoService(Processor.class)
public class FormProcessor extends AbstractProcessor {

  private RecognizerFactory recognizerFactory;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(AutoForm.class)) {
      if (!element.getKind().isClass() && !element.getKind().isInterface()) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, element + " cannot be annotated with @" + AutoForm.class.getSimpleName() + ". It may only be used on classes and interfaces");
        return true;
      }

      if (!element.asType().toString().equals("ai.swim.structure.recognizer.structural.AutoStructuralTest.IF2Impl")) {
//        continue;
      }

      ScopedContext scopedContext = new ScopedContext(processingEnv, recognizerFactory,element);

      try {
        // todo:
        //  hoist the recognizer code out to a deriveRecognizer package
        //  add a new package for deriveWritable code
        //  invoke both of the derive methods here and write out the outputs
        //  add unit tests for
        //    - simple writers
        //    - all form manipulations (header, header_body etc)
        //    - renaming
        //    - generics

        RecognizerModel recognizer = scopedContext.getRecognizer(element);

        if (recognizer == null) {
          return true;
        }

        Schema.from(recognizer).write(scopedContext);
      } catch (Throwable e) {
        e.printStackTrace();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
      }
    }

    return true;
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.recognizerFactory = RecognizerFactory.initFrom(processingEnv);
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
