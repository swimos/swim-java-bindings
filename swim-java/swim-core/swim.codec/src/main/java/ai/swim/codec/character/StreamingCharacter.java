package ai.swim.codec.character;

import java.util.Arrays;
import java.util.function.Predicate;
import ai.swim.codec.Parser;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import ai.swim.codec.source.StringSource;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;
import static ai.swim.codec.MultiParser.many0Count;
import static ai.swim.codec.ParserExt.alt;
import static ai.swim.codec.ParserExt.transpose;
import static ai.swim.codec.SequenceParser.delimited;
import static ai.swim.codec.SequenceParser.pair;

public class StreamingCharacter {

  public static Parser<Source> eqChar(char c) {
    return Parser.streaming(input -> {
      final char actual = (char) input.head();
      if (actual == c) {
        return continuation(() -> Result.ok(input.next(), Source.string(String.valueOf(actual))));
      } else {
        return none(Result.error(input, "Expected " + c));
      }
    }, 1);
  }

  public static Parser<Source> splitAtPosition(Predicate<Character> predicate, String cause) {
    return Parser.streaming(input -> {
      int head = input.head();
      StringBuilder sb = new StringBuilder();

      if (!predicate.test((char) head)) {
        return none(Result.error(input, cause));
      }

      while (predicate.test((char) head)) {
        sb.appendCodePoint(head);
        input = input.next();

        if (input.isDone()) {
          break;
        }

        head = input.head();
      }

      Source finalSource = input;
      return continuation(() -> Result.ok(finalSource, Source.string(sb.toString())));
    }, 1);
  }

  public static Parser<Source> position(Predicate<Character> predicate) {
    return Parser.streaming(input -> {
      int head = input.head();

      if (!predicate.test((char) head)) {
        Source finalSource = input;
        return continuation(() -> Result.ok(finalSource, Source.string("")));
      }

      StringBuilder sb = new StringBuilder();

      while (predicate.test((char) head)) {
        sb.appendCodePoint(head);
        input = input.next();

        if (input.isDone()) {
          break;
        }

        head = input.head();
      }

      Source finalSource = input;
      return continuation(() -> Result.ok(finalSource, Source.string(sb.toString())));
    }, 1);
  }

  public static Parser<Source> alpha0() {
    return position(Character::isLetter);
  }

  public static Parser<Source> alpha1() {
    return splitAtPosition(Character::isLetter, "Expected one or more characters");
  }

  public static Parser<Source> alphanumeric0() {
    return position(Character::isLetterOrDigit);
  }

  public static Parser<Source> alphanumeric1() {
    return splitAtPosition(Character::isLetterOrDigit, "Expected one or more numerical characters");
  }

  public static Parser<Source> digit0() {
    return position(Character::isDigit);
  }

  public static Parser<Source> digit1() {
    return splitAtPosition(Character::isDigit, "Expected one or more numerical characters");
  }

  public static Parser<Source> tag(String tag) {
    return input -> {
      int tagLength = tag.length();
      if (input.complete() || !input.has(tagLength)) {
        return none(Result.incomplete(input, tagLength - input.len()));
      } else {
        int[] next = input.borrow(tagLength);
        if (Arrays.equals(next, tag.chars().toArray())) {
          return continuation(() -> Result.ok(input.advance(tagLength), Source.string(StringSource.codePointsToString(next))));
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
        return none(Result.incomplete(input, tagLength - input.len()));
      } else {
        int[] nextChars = input.borrow(tagLength);
        int[] tagChars = tag.chars().toArray();

        if (Characters.tagNoCase(nextChars, tagChars)) {
          return continuation(() -> Result.ok(input.advance(tagLength), Source.string(StringSource.codePointsToString(nextChars))));
        } else {
          return none(Result.error(input, "Expected a tag of: " + tag));
        }
      }
    };
  }

  public static Parser<Character> satisfy(Predicate<Character> predicate) {
    return Parser.streaming(input -> {
      int head = input.head();

      if (predicate.test((char) head)) {
        return continuation(() -> Result.ok(input.next(), (char) head));
      } else {
        return none(Result.error(input, "Satisfy"));
      }
    }, 1);
  }

  public static Parser<String> oneOf(String of) {
    return Parser.streaming(input -> {
      int head = input.head();
      if (of.indexOf(head) != -1) {
        return continuation(() -> Result.ok(input.next(), String.valueOf(head)));
      } else {
        return none(Result.error(input, "One of: " + of));
      }
    }, 1);
  }

  public static Parser<Source> escape() {
    return transpose(pair(
        eqChar('\\'), satisfy(c -> {
          System.out.println(c);
          return true;
        })
    ));
  }

  @SuppressWarnings("unchecked")
  public static Parser<Source> stringLiteral() {
    return delimited(
        eqChar('"'),
        transpose(
            many0Count((alt(
                transpose(satisfy(c -> c != '\\' && c != '\"')),
                transpose(escape())
            )))
        ),
        eqChar('"')
    );
  }

}
