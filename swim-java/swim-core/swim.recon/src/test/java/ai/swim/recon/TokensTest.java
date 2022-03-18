package ai.swim.recon;

import ai.swim.codec.old.Parser3;
import ai.swim.codec.old.result.Result;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.StringInput;
import ai.swim.recon.utils.Either;
import org.junit.jupiter.api.Test;
import static ai.swim.recon.Tokens.identifier;
import static ai.swim.recon.Tokens.identifierOrBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokensTest {

  private <O> void parseOk(Parser3<O> parser, Input input, O output) {
    Result<O> result = parser.parse(input);
    assertTrue(result.isOk());
    assertEquals(output, result.getOutput());
  }

  private void parseOkStr(Parser3<Input> parser, Input input, String expectedInput, String expectedOutput) {
    Result<Input> result = parser.parse(input);
    assertTrue(result.isOk());
    assertEquals(expectedInput, StringInput.codePointsToString(result.getInput().collect()));
    assertEquals(expectedOutput, StringInput.codePointsToString(result.getOutput().collect()));
  }

  private <O> void parseIncomplete(Parser3<O> parser, Input input) {
    Result<O> result = parser.parse(input);
    assertTrue(result.isIncomplete());
  }

  @Test
  void identifierTest() {
    parseIncomplete(identifier(), Input.string("name"));
    parseOkStr(identifier(), Input.string("name "), " ", "name");
  }

  @Test
  void identifierOrBooleanTest() {
    parseIncomplete(identifierOrBoolean(), Input.string("true"));
    parseOk(identifierOrBoolean(), Input.string("true "), Either.right(true));
    parseOk(identifierOrBoolean(), Input.string("false "), Either.right(false));
    parseOk(identifierOrBoolean(), Input.string("blah "), Either.left("blah"));
  }

}