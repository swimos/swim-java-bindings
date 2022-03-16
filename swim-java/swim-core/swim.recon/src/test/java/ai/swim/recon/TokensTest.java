package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.source.Source;
import ai.swim.codec.result.Result;
import ai.swim.recon.utils.Either;
import org.junit.jupiter.api.Test;
import static ai.swim.recon.Tokens.identifier;
import static ai.swim.recon.Tokens.identifierOrBoolean;
import static org.junit.jupiter.api.Assertions.*;

class TokensTest {

  private <O> void parseOk(Parser<O> parser, Source source, O output) {
    Result<O> result = parser.parse(source);
    assertTrue(result.isOk());
    assertEquals(output, result.getOutput());
  }

  private void parseOkStr(Parser<Source> parser, Source source, String expectedInput, String expectedOutput) {
    Result<Source> result = parser.parse(source);
    assertTrue(result.isOk());
    assertEquals(expectedInput, new String(result.getInput().collect()));
    assertEquals(expectedOutput, new String(result.getOutput().collect()));
  }

  private <O> void parseIncomplete(Parser<O> parser, Source source) {
    Result<O> result = parser.parse(source);
    assertTrue(result.isIncomplete());
  }

  @Test
  void identifierTest() {
    parseIncomplete(identifier(), Source.string("name"));
    parseOkStr(identifier(), Source.string("name "),  " ", "name");
  }

  @Test
  void identifierOrBooleanTest() {
    parseIncomplete(identifierOrBoolean(), Source.string("true"));
    parseOk(identifierOrBoolean(), Source.string("true "), Either.right(true));
    parseOk(identifierOrBoolean(), Source.string("false "), Either.right(false));
    parseOk(identifierOrBoolean(), Source.string("blah "), Either.left("blah"));
  }

}