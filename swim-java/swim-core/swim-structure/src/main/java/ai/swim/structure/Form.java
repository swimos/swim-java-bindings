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

package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.proxy.RecognizerProxy;
import ai.swim.structure.value.Value;
import ai.swim.structure.writer.StructuralWriter;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.proxy.WriterProxy;
import java.util.function.Function;

public final class Form<T> extends Recognizer<T> implements Writable<T> {
  private final Recognizer<T> recognizer;
  private final Writable<T> writable;

  public Form(Recognizer<T> recognizer, Writable<T> writable) {
    this.recognizer = recognizer;
    this.writable = writable;
  }

  public static <T> Form<T> forClass(Class<T> tClass) {
    return new Form<>(RecognizerProxy.getProxy().lookup(tClass), WriterProxy.getProxy().lookup(tClass));
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    return recognizer.feedEvent(event);
  }

  @Override
  public Recognizer<T> reset() {
    return recognizer.reset();
  }

  @Override
  public <T1> T1 writeInto(T from, StructuralWriter<T1> structuralWriter) {
    return writable.writeInto(from, structuralWriter);
  }

  @Override
  public <Y> Recognizer<Y> map(Function<T, Y> mapFn) {
    return recognizer.map(mapFn);
  }

  @Override
  public Recognizer<T> required() {
    return recognizer.required();
  }

  @Override
  public Recognizer<T> asAttrRecognizer() {
    return recognizer.asAttrRecognizer();
  }

  @Override
  public Recognizer<T> asBodyRecognizer() {
    return recognizer.asBodyRecognizer();
  }

  @Override
  public <W> T transform(W value, Writable<W> writable) {
    return recognizer.transform(value, writable);
  }

  @Override
  public <W> T transform(W value) {
    return recognizer.transform(value);
  }

  @Override
  public T flush() {
    return recognizer.flush();
  }

  @Override
  public Value asValue(T value) {
    return writable.asValue(value);
  }

  @Override
  public boolean isCont() {
    return recognizer.isCont();
  }

  @Override
  public boolean isDone() {
    return recognizer.isDone();
  }

  @Override
  public boolean isError() {
    return recognizer.isError();
  }

  @Override
  public T bind() {
    return recognizer.bind();
  }

  @Override
  public RuntimeException trap() {
    return recognizer.trap();
  }

  @Override
  public String asReconString(T value) {
    return writable.asReconString(value);
  }

  @Override
  public String asCompactReconString(T value) {
    return writable.asCompactReconString(value);
  }

  @Override
  public String asPrettyReconString(T value) {
    return writable.asPrettyReconString(value);
  }

}
