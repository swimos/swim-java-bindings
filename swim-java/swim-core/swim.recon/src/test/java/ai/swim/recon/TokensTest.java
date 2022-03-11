package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.codec.result.Result;
import org.junit.jupiter.api.Test;
import static ai.swim.recon.Tokens.identifier;
import static org.junit.jupiter.api.Assertions.*;

class TokensTest {

  private <O> void parseOk(Parser<O> parser, Input input) {
    Result<O> result = parser.parse(input);
    assertTrue(result.isOk());
  }

  private <O> void parseIncomplete(Parser<O> parser, Input input) {
    Result<O> result = parser.parse(input);
    assertTrue(result.isIncomplete());
  }

  @Test
  void identifierTest() {
    parseIncomplete(identifier(), Input.string("name"));
    parseOk(identifier(), Input.string("name "));
  }

}