package ai.swim.codec;

import ai.swim.codec.result.ParseError;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserExt.alt;
import static ai.swim.codec.ParserExt.transpose;
import static ai.swim.codec.ParserTestUtils.runParserIncomplete;
import static ai.swim.codec.ParserTestUtils.runParserOk;
import static ai.swim.codec.SequenceParser.separatedPair;
import static ai.swim.codec.character.StreamingCharacter.alpha1;
import static ai.swim.codec.character.StreamingCharacter.eqChar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserExtTest {

  void recognizeOk(Parser<Source> parser, String input, Source expectedSource, Source expectedOutput) {
    Result<Source> result = parser.parse(Source.string(input));
    assertTrue(result.isOk());

    assertEquals(new String(expectedOutput.collect()), new String(result.getInput().collect()));
    assertEquals(new String(expectedSource.collect()), new String(result.getOutput().collect()));
  }

  void recognizeErr(Parser<Source> parser, String input, String cause, Location location) {
    Result<Source> result = parser.parse(Source.string(input));
    assertTrue(result.isError());

    ParseError<Source> error = (ParseError<Source>) result;

    assertEquals(cause, error.getCause());
    assertEquals(location, error.getLocation());
  }

  @Test
  void recognizeTest() {
    Parser<Source> recognize = transpose(separatedPair(alpha1(), eqChar(','), alpha1()));
    recognizeOk(recognize, "abcd,efgh", Source.string("abcd,efgh"), Source.string(""));
    recognizeOk(recognize, "abcd,efgh;123", Source.string("abcd,efgh"), Source.string(";123"));
    recognizeErr(recognize, "abcd;", "Expected ,", Location.of(1, 5));
  }

  @Test
  @SuppressWarnings("unchecked")
  void altTest() {
    Parser<Source> parser = alt(
        eqChar('a'),
        eqChar('b'),
        eqChar('c')
    );

    runParserOk(parser, "a", "a", "");
    runParserOk(parser, "b", "b", "");
    runParserOk(parser, "c", "c", "");
    runParserOk(parser, "abcd", "a", "bcd");

    runParserIncomplete(parser, "", 1);
  }

}