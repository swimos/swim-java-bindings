package ai.swim.codec;

import ai.swim.codec.input.Input;
import ai.swim.codec.result.Result;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class MultiParser {

  public static <O> Parser<Integer> many0Count(Parser<O> parser) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        Input localInput = input;
        int count = 0;

        while (true) {
          int oldLength = localInput.len();
          Result<O> parseResult = parser.parse(input);

          if (parseResult.isError()) {
            int finalCount = count;
            return continuation(() -> Result.ok(input, finalCount));
          } else if (parseResult.isIncomplete()) {
            return none(Result.incomplete(input, 1));
          } else {
            localInput = parseResult.getInput();
            count += 1;
            if (input.len() == oldLength) {
              return none(Result.error(input, "Parser did not consume"));
            }
          }
        }
      }
    };
  }

  public static <O> Parser<Integer> many1Count(Parser<O> parser) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        return parser.parse(input).match(
            ok -> {
              Input localInput = ok.getInput();
              int count = 1;

              while (true) {
                int oldLength = localInput.len();
                Result<O> parseResult = parser.parse(input);

                if (parseResult.isError() || parseResult.isIncomplete()) {
                  int finalCount = count;
                  return continuation(() -> Result.ok(input, finalCount));
                } else if (parseResult.isOk()) {
                  localInput = parseResult.getInput();
                  count += 1;
                  if (input.len() == oldLength) {
                    return none(Result.error(input, "Parser did not consume"));
                  }
                }
              }
            },
            err -> none(Result.error(input, null)),
            in -> none(Result.incomplete(input, 1))
        );
      }
    };
  }

}