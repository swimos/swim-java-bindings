package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

import java.util.List;
import java.util.stream.Collectors;

public class ObjectRecognizer extends Recognizer<Object> {

  private List<Recognizer<?>> recognizers;

  public ObjectRecognizer() {

  }

  @Override
  public Recognizer<Object> feedEvent(ReadEvent event) {
    if (this.recognizers == null) {
      // todo: Horribly inefficient
      // this can't be done in the constructor as some of the recognizers may depend on this one
      this.recognizers = RecognizerProxy.getInstance()
          .getAllRecognizers()
          .stream()
          .filter(e -> !e.getKey().equals(Object.class))
          .map(e -> e.getValue().get())
          .collect(Collectors.toList());
    }

    for (int i = 0; i < this.recognizers.size(); i++) {
      Recognizer<?> recognizer = this.recognizers.get(i);

      if (recognizer.isError()) {
        continue;
      }

      recognizer = recognizer.feedEvent(event);

      if (recognizer.isDone()) {
        return Recognizer.done(recognizer.bind(), this);
      } else {
        this.recognizers.set(i, recognizer);
      }
    }

    return Recognizer.error(new RuntimeException("No recognizers produced a value"));
  }

  @Override
  public Recognizer<Object> reset() {
    return new ObjectRecognizer();
  }
}
