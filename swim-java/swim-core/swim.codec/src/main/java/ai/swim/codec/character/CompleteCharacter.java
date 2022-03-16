package ai.swim.codec.character;

import ai.swim.codec.Parser;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import java.util.Arrays;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class CompleteCharacter {

  public static Parser<Source> tag(String tag) {
    return input -> {
      int tagLength = tag.length();
      if (input.complete() || !input.has(tagLength)) {
        return none(Result.error(input, null));
      } else {
        char[] next = input.borrow(tagLength);
        if (Arrays.equals(next, tag.toCharArray())) {
          return continuation(() -> Result.ok(input.advance(tagLength), Source.string(new String(next))));
        } else {
          return none(Result.error(input, "Expected a tag of: " + tag));
        }
      }
    };
  }

  public static Parser<Source> tagNoCase(String tag) {
    return input -> {
      int tagLength = tag.length();
      if (input.complete() || !input.has(tagLength)) {
        return none(Result.error(input, null));
      } else {
        char[] nextChars = input.borrow(tagLength);
        char[] tagChars = tag.toCharArray();

        if (CharacterImpl.tagNoCase(nextChars, tagChars)) {
          return continuation(() -> Result.ok(input.advance(tagLength), Source.string(new String(nextChars))));
        } else {
          return none(Result.error(input, "Expected a tag of: " + tag));
        }
      }
    };
  }

  public static Parser<Source> oneOf(String of) {
    return Parser.complete(input -> {
      char head = input.head();
      if (of.indexOf(head) != -1) {
        return continuation(() -> Result.ok(input.next(), Source.string(String.valueOf(head))));
      } else {
        return none(Result.error(input, "One of: " + of));
      }
    });
  }

}
