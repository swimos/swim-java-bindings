package ai.swim.codec.character;

import java.util.Arrays;
import ai.swim.codec.Parser;
import ai.swim.codec.result.Result;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class CompleteCharacter {

  public static Parser<String> tag(String tag) {
    return input -> {
      int tagLength = tag.length();
      if (input.complete() || !input.has(tagLength)) {
        return none(Result.error(input, null));
      } else {
        char[] next = input.borrow(tagLength);
        if (Arrays.equals(next, tag.toCharArray())) {
          return continuation(() -> Result.ok(input.advance(tagLength), new String(next)));
        } else {
          return none(Result.error(input, "Expected a tag of: " + tag));
        }
      }
    };
  }

}
