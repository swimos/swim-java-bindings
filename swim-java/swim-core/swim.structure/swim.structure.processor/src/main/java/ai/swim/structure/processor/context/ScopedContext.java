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

package ai.swim.structure.processor.context;

import ai.swim.structure.processor.recognizer.RecognizerFactory;
import ai.swim.structure.processor.writer.WriterFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;

public class ScopedContext {
  private final Element root;
  private final ScopedMessager messager;
  private final NameFactory formatter;
  private final RecognizerFactory recognizerFactory;
  private final WriterFactory writerFactory;
  private final ProcessingEnvironment processingEnvironment;

  public ScopedContext(ProcessingEnvironment processingEnvironment, RecognizerFactory recognizerFactory,WriterFactory writerFactory, Element root) {
    this.root = root;
    this.messager = new ScopedMessager(processingEnvironment.getMessager(), root);

    Elements elementUtils = processingEnvironment.getElementUtils();
    PackageElement packageElement = elementUtils.getPackageOf(root);

    this.formatter = new NameFactory(root.getSimpleName().toString(), packageElement);
    this.recognizerFactory = recognizerFactory;
    this.writerFactory=writerFactory;
    this.processingEnvironment = processingEnvironment;
  }

  public Element getRoot() {
    return root;
  }

  public ScopedMessager getMessager() {
    return messager;
  }

  public RecognizerFactory getRecognizerFactory() {
    return recognizerFactory;
  }

  public WriterFactory getWriterFactory() {
    return writerFactory;
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  public NameFactory getNameFactory() {
    return formatter;
  }

}
