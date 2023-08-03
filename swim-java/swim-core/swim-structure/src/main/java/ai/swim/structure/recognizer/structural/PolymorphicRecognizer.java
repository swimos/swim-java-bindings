package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PolymorphicRecognizer<T> extends StructuralRecognizer<T> {

  private final List<Recognizer<? extends T>> recognizers;
  private Recognizer<? extends T> current;

  public PolymorphicRecognizer(List<Recognizer<? extends T>> recognizers) {
    Objects.requireNonNull(recognizers);
    if (recognizers.isEmpty()) {
      throw new IllegalArgumentException("Cannot initialise a polymorphic recognizer with no recognizers");
    }
    this.recognizers = new ArrayList<>(recognizers);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (current != null) {
      current = current.feedEvent(event);
      if (current.isDone()) {
        return Recognizer.done(current.bind(), this);
      } else if (current.isError()) {
        return Recognizer.error(current.trap());
      } else {
        return this;
      }
    } else {
      for (Recognizer<? extends T> recognizer : recognizers) {
        Recognizer<? extends T> activeRecognizer = recognizer.feedEvent(event);
        if (activeRecognizer.isCont()) {
          current = activeRecognizer;
          return this;
        } else if (activeRecognizer.isDone()) {
          return Recognizer.done(activeRecognizer.bind(), this);
        }
      }
    }

    return Recognizer.error(new RuntimeException("Tag mismatch"));
  }

  @Override
  public Recognizer<T> reset() {
    recognizers.replaceAll(Recognizer::reset);
    this.current = null;
    return this;
  }

  @Override
  public boolean isCont() {
    if (current == null) {
      return super.isCont();
    } else {
      return current.isCont();
    }
  }

  @Override
  public boolean isDone() {
    if (current == null) {
      return super.isDone();
    } else {
      return current.isDone();
    }
  }

  @Override
  public boolean isError() {
    if (current == null) {
      return super.isError();
    } else {
      return current.isError();
    }
  }

  @Override
  public T bind() {
    if (current == null) {
      return super.bind();
    } else {
      return current.bind();
    }
  }

  @Override
  public RuntimeException trap() {
    if (current == null) {
      return super.trap();
    } else {
      return current.trap();
    }
  }
}
