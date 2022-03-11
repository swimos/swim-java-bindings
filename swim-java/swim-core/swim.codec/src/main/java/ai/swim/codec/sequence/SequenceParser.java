package ai.swim.codec.sequence;

import ai.swim.codec.Parser;
import ai.swim.codec.result.ParseOk;
import ai.swim.codec.result.Result;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class SequenceParser {

  public static <O1, O2, O3> Parser<Pair<O1, O3>> separatedPair(Parser<O1> first, Parser<O2> sep, Parser<O3> second) {
    return input -> {
      if (input.isDone()) {
        return none(Result.incomplete(input, 1));
      } else {
        Result<Pair<O1, O3>> result = first.parse(input).mapOk((ParseOk<O1> ok1) ->
            sep.parse(ok1.getInput()).mapOk((ParseOk<O2> ok2) ->
                second.parse(ok2.getInput())
                    .mapOk(ok3 -> Result.ok(ok3.getInput(), new Pair<>(ok1.getOutput(), ok3.getOutput())).cast()))
        );

        if (result.isOk()) {
          return continuation(() -> result);
        } else {
          return none(result);
        }
      }
    };
  }

  public static <O1, O2> Parser<Pair<O1, O2>> pair(Parser<O1> first, Parser<O2> second) {
    return input -> {
      if (input.isDone()) {
        return none(Result.incomplete(input, 1));
      } else {
        Result<Pair<O1, O2>> result = first.parse(input)
            .mapOk((ParseOk<O1> ok1) -> second.parse(ok1.getInput())
                .mapOk((ParseOk<O2> ok2) -> Result.ok(ok2.getInput(), new Pair<>(ok1.getOutput(), ok2.getOutput())))
            );

        if (result.isOk()) {
          return continuation(() -> result);
        } else {
          return none(result);
        }
      }
    };
  }

  public static class Pair<O1, O2> {

    private final O1 output1;
    private final O2 output2;

    private Pair(O1 output1, O2 output2) {
      this.output1 = output1;
      this.output2 = output2;
    }

    public O1 getOutput1() {
      return output1;
    }

    public O2 getOutput2() {
      return output2;
    }

  }

}
