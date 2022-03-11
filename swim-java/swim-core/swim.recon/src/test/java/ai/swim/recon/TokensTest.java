package ai.swim.recon;

import java.util.Arrays;
import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.codec.result.Result;
import ai.swim.recon.utils.Either;
import org.junit.jupiter.api.Test;
import static ai.swim.recon.Tokens.identifier;
import static ai.swim.recon.Tokens.identifierOrBoolean;
import static org.junit.jupiter.api.Assertions.*;

class TokensTest {

  private <O> void parseOk(Parser<O> parser, Input input, O output) {
    Result<O> result = parser.parse(input);
    assertTrue(result.isOk());
    assertEquals(output, result.getOutput());
    System.out.println(new String(result.getInput().collect()));
  }

  private <O> void parseIncomplete(Parser<O> parser, Input input) {
    Result<O> result = parser.parse(input);
    assertTrue(result.isIncomplete());
  }

  @Test
  void identifierTest() {
    parseIncomplete(identifier(), Input.string("name"));
    parseOk(identifier(), Input.string("name "), Input.string(""));
  }

  @Test
  void identifierOrBooleanTest() {
    parseIncomplete(identifierOrBoolean(), Input.string("true"));
    parseOk(identifierOrBoolean(), Input.string("true "), Either.right(true));
    parseOk(identifierOrBoolean(), Input.string("false "), Either.right(false));
    parseOk(identifierOrBoolean(), Input.string("blah "), Either.left("blah"));
  }

}