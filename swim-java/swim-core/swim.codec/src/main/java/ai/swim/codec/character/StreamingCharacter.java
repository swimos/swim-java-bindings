package ai.swim.codec.character;

import java.util.Arrays;
import java.util.function.Predicate;
import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.codec.result.Result;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class StreamingCharacter {

  public static Parser<String> eqChar(char c) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        final char actual = input.head();
        if (actual == c) {
          return continuation(() -> Result.ok(input.next(), String.valueOf(actual)));
        } else {
          return none(Result.error(input, "Expected " + c));
        }
      }
    };
  }

  public static Parser<String> splitAtPosition(Predicate<Character> predicate, String cause) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        char head = input.head();
        StringBuilder sb = new StringBuilder();

        if (!predicate.test(head)) {
          return none(Result.error(input, cause));
        }

        while (predicate.test(head)) {
          sb.append(head);
          input = input.next();

          if (input.isDone()) {
            break;
          }

          head = input.head();
        }

        Input finalInput = input;
        return continuation(() -> Result.ok(finalInput, sb.toString()));
      }
    };
  }

  public static Parser<String> position(Predicate<Character> predicate) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        char head = input.head();

        if (!predicate.test(head)) {
          Input finalInput = input;
          return continuation(() -> Result.ok(finalInput, ""));
        }

        StringBuilder sb = new StringBuilder();

        while (predicate.test(head)) {
          sb.append(head);
          input = input.next();

          if (input.isDone()) {
            break;
          }

          head = input.head();
        }

        Input finalInput = input;
        return continuation(() -> Result.ok(finalInput, sb.toString()));
      }
    };
  }

  public static Parser<String> alpha0() {
    return position(Character::isLetter);
  }

  public static Parser<String> alpha1() {
    return splitAtPosition(Character::isLetter, "Expected one or more characters");
  }

  public static Parser<String> alphanumeric0() {
    return position(Character::isLetterOrDigit);
  }

  public static Parser<String> alphanumeric1() {
    return splitAtPosition(Character::isLetterOrDigit, "Expected one or more numerical characters");
  }

  public static Parser<String> digit0() {
    return position(Character::isDigit);
  }

  public static Parser<String> digit1() {
    return splitAtPosition(Character::isDigit, "Expected one or more numerical characters");
  }

  public static Parser<String> tag(String tag) {
    return input -> {
      int tagLength = tag.length();
      if (input.complete() || !input.has(tagLength)) {
        return none(Result.incomplete(input, tagLength-input.len() ));
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

  public static Parser<String> satisfy(Predicate<Character> predicate) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        char head = input.head();

        if (predicate.test(head)) {
          return continuation(()->Result.ok(input.next(), String.valueOf(head)));
        } else {
          return none(Result.error(input, "Satisfy"));
        }
      }
    };
  }

}
