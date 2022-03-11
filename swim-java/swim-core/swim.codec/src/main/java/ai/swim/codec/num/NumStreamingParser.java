package ai.swim.codec.num;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import static ai.swim.codec.MultiParser.many1Count;
import static ai.swim.codec.ParserExt.preceded;
import static ai.swim.codec.ParserExt.recognize;
import static ai.swim.codec.character.StreamingCharacter.oneOf;
import static ai.swim.codec.character.StreamingCharacter.tagNoCase;

public class NumStreamingParser {

  public static Parser<Input> natural(String tag, String digits) {
    return preceded(
        tagNoCase(tag),
        recognize(many1Count(oneOf(digits)))
    );
  }

}
