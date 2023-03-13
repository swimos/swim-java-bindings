package ai.swim.codec.parsers.text;

import ai.swim.codec.Parser;
import ai.swim.codec.parsers.combinators.TakeWhile0;

public class Multispace0 extends TakeWhile0<StringBuilder, String> {
  private Multispace0() {
    super(new StringBuilder());
  }

  public static Parser<String> multispace0() {
    return new Multispace0();
  }

  @Override
  protected boolean onAdvance(int c, StringBuilder state) {
    if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
      state.appendCodePoint(c);
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected String onDone(StringBuilder state) {
    return state.toString();
  }
}
