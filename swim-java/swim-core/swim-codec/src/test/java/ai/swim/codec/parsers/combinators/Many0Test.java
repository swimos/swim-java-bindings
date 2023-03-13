package ai.swim.codec.parsers.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ai.swim.codec.LambdaParser.lambda;
import static ai.swim.codec.parsers.MapReduce.mapReduce;
import static ai.swim.codec.parsers.combinators.Many0.many0;
import static ai.swim.codec.parsers.text.EqChar.eqChar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Many0Test {

  void many0Ok(String input, String expectedChars) {
    Parser<List<Character>> parser = many0(eqChar('a'));
    parser = parser.feed(Input.string(input));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), expectedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList()));
  }

  @Test
  void many0TestOk() {
    many0Ok("aaaabcde", "aaaa");
    many0Ok("a", "a");
    many0Ok("", "");
    many0Ok(" a", "");
    many0Ok("bcd", "");

    Parser<String> parser = mapReduce(many0(eqChar('a'))).feed(Input.string("aaaabcd"));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "aaaa");
  }

  void many0Cont(String input, String expectedChars) {
    Parser<List<Character>> parser = many0(eqChar('a'));

    for (char c : input.toCharArray()) {
      parser = parser.feed(Input.string(String.valueOf(c)).setPartial(true));

      if (c == 'a') {
        assertFalse(parser.isDone());
        assertFalse(parser.isError());
      } else {
        assertTrue(parser.isDone());
        break;
      }
    }

    if (!parser.isDone()) {
      parser = parser.feed(Input.string(""));
    }

    assertTrue(parser.isDone());
    assertEquals(parser.bind(), expectedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList()));
  }

  @Test
  void many0TestCont() {
    many0Cont("aaaabcde", "aaaa");
    many0Cont("a", "a");
    many0Cont("", "");
    many0Cont(" a", "");
    many0Cont("bcd", "");
  }

  @Test
  void many0TestErr() {
    Parser<List<Character>> parser = many0(eqChar('a').andThen(c -> Parser.error(Input.string(""), "err")));
    parser = parser.feed(Input.string("abc"));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), Collections.emptyList());

    Parser<List<Character>> p2 = many0(lambda(input -> {
      char c = (char) input.head();
      if (c != 'a') {
        return Parser.error(input, String.valueOf(c));
      } else {
        input.step();
        return Parser.done(c);
      }
    }));

    p2 = p2.feed(Input.string("aabcd"));
    assertTrue(p2.isDone());
    assertEquals(p2.bind(), List.of('a', 'a'));
  }
}