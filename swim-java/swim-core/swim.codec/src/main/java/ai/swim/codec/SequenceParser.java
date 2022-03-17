package ai.swim.codec;

import ai.swim.codec.models.Pair;
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

  public static <O1, O2, O3> Parser<O2> delimited(Parser<O1> first, Parser<O2> second, Parser<O3> third) {
    return input -> {
      if (input.isDone()) {
        return none(Result.incomplete(input, 1));
      } else {
        Result<O2> result = first.parse(input).mapOk(
            ok1 -> second.parse(ok1.getInput()).mapOk(
                ok2 -> third.parse(ok2.getInput()).mapOk(
                    ok3 -> Result.ok(ok3.getInput(), ok2.getOutput())
                )
            )
        );

        if (result.isOk()) {
          return continuation(() -> result);
        } else {
          return none(result);
        }
      }
    };
  }

}