package ai.swim.structure.recognizer.std.collections;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.FirstOf;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.SimpleAttrBodyRecognizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListRecognizer<E> extends CollectionRecognizer<E, List<E>> {
  @AutoForm.TypedConstructor
  public ListRecognizer(Recognizer<E> delegate) {
    super(delegate, new ArrayList<>());
  }

  public ListRecognizer(Recognizer<E> delegate, boolean isAttrBody) {
    super(delegate, new ArrayList<>(), isAttrBody);
  }

  @Override
  public Recognizer<List<E>> reset() {
    return new ListRecognizer<>(this.delegate.reset(), isAttrBody);
  }

  @Override
  public Recognizer<List<E>> asAttrRecognizer() {
    return new FirstOf<>(
        new ListRecognizer<>(this.delegate.reset(), true),
        new SimpleAttrBodyRecognizer<>(new ListRecognizer<>(this.delegate.reset(), false))
    );
  }
}
