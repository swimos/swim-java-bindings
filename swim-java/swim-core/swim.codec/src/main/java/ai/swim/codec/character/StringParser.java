package ai.swim.codec.character;

import java.util.function.Predicate;
import ai.swim.codec.Parser;
import ai.swim.codec.Result;
import ai.swim.codec.input.Input;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class StringParser {

  public static Parser<Character, String> eqChar(char c) {
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

  public static Parser<Character, String> splitAtPosition(Predicate<Character> predicate, String cause) {
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

        Input<Character> finalInput = input;
        return continuation(() -> Result.ok(finalInput, sb.toString()));
      }
    };
  }

  public static Parser<Character, String> position(Predicate<Character> predicate) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        char head = input.head();

        if (!predicate.test(head)) {
          Input<Character> finalInput = input;
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

        Input<Character> finalInput = input;
        return continuation(() -> Result.ok(finalInput, sb.toString()));
      }
    };
  }

  public static Parser<Character, String> alpha0() {
    return position(Character::isLetter);
  }

  public static Parser<Character, String> alpha1() {
    return splitAtPosition(Character::isLetter, "Expected one or more characters");
  }

  public static Parser<Character, String> alphanumeric0() {
    return position(Character::isLetterOrDigit);
  }

  public static Parser<Character, String> alphanumeric1() {
    return splitAtPosition(Character::isLetterOrDigit, "Expected one or more numerical characters");
  }

  public static Parser<Character, String> digit0() {
    return position(Character::isDigit);
  }

  public static Parser<Character, String> digit1() {
    return splitAtPosition(Character::isDigit, "Expected one or more numerical characters");
  }

}
