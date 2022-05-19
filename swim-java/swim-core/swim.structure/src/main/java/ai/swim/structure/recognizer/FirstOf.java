package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

public class FirstOf<T> extends Recognizer<T> {
  enum State {
    Both,
    Left,
    Right
  }

  private Recognizer<T> left;
  private Recognizer<T> right;
  private State state;

  public FirstOf(Recognizer<T> left, Recognizer<T> right) {
    this.left = left;
    this.right = right;
    this.state = State.Both;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (state) {
      case Both:
        left = left.feedEvent(event);
        if (left.isDone()) {
          return Recognizer.done(left.bind(), this);
        } else if (left.isError()) {
          state = State.Right;
        }

        right = right.feedEvent(event);
        if (right.isDone()) {
          return Recognizer.done(right.bind(), this);
        } else if (right.isError()) {
          if (state == State.Both) {
            state = State.Left;
            return this;
          } else {
            return Recognizer.error(right.trap());
          }
        } else {
          return this;
        }
      case Left:
        left = left.feedEvent(event);
        return discriminate(left);
      case Right:
        right = right.feedEvent(event);
        return discriminate(right);
      default:
        throw new AssertionError();
    }
  }

  private Recognizer<T> discriminate(Recognizer<T> recognizer) {
    if (recognizer.isDone()) {
      return Recognizer.done(recognizer.bind(), this);
    } else if (recognizer.isError()) {
      return Recognizer.error(recognizer.trap());
    } else {
      return this;
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new FirstOf<>(left.reset(), right.reset());
  }
}
