package ai.swim.codec.parsers.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.StringInput;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakeWhile0Test {

  @Test
  void takeWhile0Test() {
    Parser<String> parser = new LambdaTakeWhile(Character::isDigit);
    Input input = Input.string("12345abcde");

    parser = parser.feed(input);
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "12345");

    assertEquals(StringInput.codePointsToString(input.bind()), "abcde");
  }

  private static class LambdaTakeWhile extends TakeWhile0<StringBuilder, String> {

    private final Predicate<Character> predicate;

    public LambdaTakeWhile(Predicate<Character> predicate) {
      super(new StringBuilder());
      this.predicate = predicate;
    }

    @Override
    protected boolean onAdvance(int c, StringBuilder state) {
      if (predicate.test((char) c)) {
        state.appendCodePoint(c);
        return true;
      }

      return false;
    }

    @Override
    protected String onDone(StringBuilder state) {
      return state.toString();
    }
  }
}