package ai.swim.codec;

import ai.swim.codec.input.Input;
import ai.swim.codec.result.ParseError;
import ai.swim.codec.result.Result;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserExt.recognize;
import static ai.swim.codec.StringParser.alpha1;
import static ai.swim.codec.StringParser.eqChar;
import static ai.swim.codec.SequenceParser.separatedPair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserExtTest {

  void recognizeOk(Parser<Input> parser, String input, Input expectedInput, Input expectedOutput) {
    Result<Input> result = parser.parse(Input.string(input));
    assertTrue(result.isOk());

    assertEquals(new String(expectedInput.collect()), new String(result.getInput().collect()));
    assertEquals(new String(expectedOutput.collect()), new String(result.getOutput().collect()));
  }

  void recognizeErr(Parser<Input> parser, String input, String cause, Location location) {
    Result<Input> result = parser.parse(Input.string(input));
    assertTrue(result.isError());

    ParseError<Input> error = (ParseError<Input>) result;

    assertEquals(cause,error.getCause());
    assertEquals(location, error.getLocation());
  }

  @Test
  void recognizeTest() {
    Parser<Input> recognize = recognize(separatedPair(alpha1(), eqChar(','), alpha1()));
    recognizeOk(recognize, "abcd,efgh", Input.string("abcd,efgh"), Input.string(""));
    recognizeOk(recognize, "abcd,efgh;123", Input.string("abcd,efgh"), Input.string(";123"));
    recognizeErr(recognize, "abcd;", "Expected ,", Location.of(1,5));
  }

}