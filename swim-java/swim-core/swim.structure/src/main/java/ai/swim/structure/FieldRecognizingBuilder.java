package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.recognizer.proxy.RecognizerProxy;

public class FieldRecognizingBuilder<I> implements RecognizingBuilder<I> {

  public Recognizer<I> recognizer;
  public I value;

  public FieldRecognizingBuilder(Class<I> clazz) {
    this.recognizer = RecognizerProxy.getProxy().lookup(clazz);
  }

  public FieldRecognizingBuilder(Recognizer<I> recognizer) {
    this.recognizer = recognizer;
  }

  @Override
  public boolean feed(ReadEvent event) {
    if (this.value != null) {
      throw new RecognizerException("Duplicate value");
    }

    Recognizer<I> feedResult = this.recognizer.feedEvent(event);
    if (feedResult.isDone()) {
      value = feedResult.bind();
      return true;
    } else if (feedResult.isError()) {
      throw feedResult.trap();
    } else {
      this.recognizer = feedResult;
      return false;
    }
  }

  @Override
  public I bind() {
    return this.value;
  }

  @Override
  public I bindOr(I defaultValue) {
    if (this.value == null) {
      return defaultValue;
    } else {
      return this.value;
    }
  }

  @Override
  public RecognizingBuilder<I> reset() {
    return new FieldRecognizingBuilder<>(this.recognizer.reset());
  }

}
