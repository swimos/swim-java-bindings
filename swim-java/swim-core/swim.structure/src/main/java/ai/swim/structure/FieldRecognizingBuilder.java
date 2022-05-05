package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerProxy;

public class FieldRecognizingBuilder<I> implements RecognizingBuilder<I> {

  public final Recognizer<I> recognizer;
  public I value;

  public FieldRecognizingBuilder(Class<I> clazz) {
    this.recognizer = RecognizerProxy.getInstance().lookup(clazz);
  }

  public FieldRecognizingBuilder(Recognizer<I> recognizer) {
    this.recognizer = recognizer;
  }

  @Override
  public boolean feed(ReadEvent event) {
    if (this.value != null) {
      throw new RuntimeException("Duplicate value");
    }

    Recognizer<I> feedResult = this.recognizer.feedEvent(event);
    if (feedResult.isDone()) {
      value = feedResult.bind();
      return true;
    } else if (feedResult.isError()) {
      throw feedResult.trap();
    } else {
      return false;
    }
  }

  @Override
  public I bind() {
    return this.value;
  }

  @Override
  public RecognizingBuilder<I> reset() {
    return new FieldRecognizingBuilder<>(this.recognizer.reset());
  }

}
