package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;
import java.util.function.Function;

public class MappingRecognizer<I, O> extends Recognizer<O> {

  private final Function<I, O> mapFn;
  private Recognizer<I> delegate;

  public MappingRecognizer(Recognizer<I> delegate, Function<I, O> mapFn) {
    this.delegate = delegate;
    this.mapFn = mapFn;
  }

  @Override
  public Recognizer<O> feedEvent(ReadEvent event) {
    this.delegate = this.delegate.feedEvent(event);
    if (delegate.isDone()) {
      return Recognizer.done(this.mapFn.apply(this.delegate.bind()), this);
    } else if (this.delegate.isError()) {
      return Recognizer.error(this.delegate.trap());
    } else {
      return this;
    }
  }

  @Override
  public Recognizer<O> reset() {
    return new MappingRecognizer<>(this.delegate.reset(), this.mapFn);
  }
}
