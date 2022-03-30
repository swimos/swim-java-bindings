package ai.swim.codec.parsers;

import ai.swim.codec.Parser;
import ai.swim.codec.input.InputError;

import java.util.Arrays;

import static ai.swim.codec.parsers.ParserExt.peek;

public class StringParsersExt {

  public static Parser<Character> eqChar(char c) {
    return Parser.lambda(input -> {
      if (input.isContinuation()) {
        char head = (char) input.head();
        if (head == c) {
          input.step();
          return Parser.done(head);
        } else {
          return Parser.error("Expected: " + c + ", found: " + head);
        }
      } else if (input.isError()) {
        return Parser.error(((InputError) input).getCause());
      } else if (input.isDone()) {
        return Parser.error("Insufficient data");
      } else {
        return eqChar(c);
      }
    });
  }

  public static Parser<Character> oneOf(char... chars) {
    return Parser.lambda(input -> {
      if (input.isContinuation()) {
        char head = (char) input.head();

        for (char c : chars) {
          if (head == c) {
            input.step();
            return Parser.done(head);
          }
        }

        return Parser.error("Expected one of: " + Arrays.toString(chars));
      } else if (input.isError()) {
        return Parser.error(((InputError) input).getCause());
      } else if (input.isDone()) {
        return Parser.error("Insufficient data");
      } else {
        return oneOf(chars);
      }
    });
  }

  public static Parser<String> lineEnding() {
    return Parser.preceded(peek(oneOf('\r', '\n')), Parser.lambda(input -> {
      if (input.isContinuation()) {
        char first = (char) input.head();
        if (first == '\n') {
          input.step();
          return Parser.done("\n");
        } else if (first == '\r') {
          input = input.step();
          if (input.isContinuation()) {
            char second = (char) input.head();
            if (second == '\n') {
              input.step();
              return Parser.done("\r\n");
            } else {
              return Parser.error("Expected a line ending");
            }
          } else {
            return lineEnding();
          }
        } else {
          return Parser.error("Expected a line ending");
        }
      } else if (input.isEmpty()) {
        return Parser.error("Need more data");
      } else if (input.isDone()) {
        return Parser.error("Invalid input");
      } else {
        return lineEnding();
      }
    }));
  }

}
