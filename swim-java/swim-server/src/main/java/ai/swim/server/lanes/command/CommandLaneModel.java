package ai.swim.server.lanes.command;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.input.Input;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import ai.swim.structure.FormParser;
import ai.swim.structure.recognizer.RecognizerException;
import java.util.UUID;

public final class CommandLaneModel<T> extends LaneModel {
  private final CommandLaneView<T> view;
  private final Form<T> form;
  private final CommandState<T> state;

  public CommandLaneModel(int laneId, CommandLaneView<T> view, StateCollector collector) {
    this.view = view;
    this.form = view.valueForm();
    this.state = new CommandState<>(laneId, view.valueForm(), collector);
  }

  @Override
  public void dispatch(ReadBuffer buffer) {
    Parser<T> parser = new FormParser<>(form.reset());
    parser = parser.feed(Input.readBuffer(buffer));

    if (parser.isDone()) {
      T value = parser.bind();
      view.onCommand(value);
      state.command(value);
    } else if (parser.isError()) {
      ParserError<T> error = (ParserError<T>) parser;
      throw new RecognizerException(String.format("%s at: %s", error.cause(), error.location()));
    } else {
      throw new RecognizerException("Unconsumed input");
    }
  }

  @Override
  public void sync(UUID remote) {
    state.sync(remote);
  }

  @Override
  public void init(ReadBuffer buffer) {
    // no-op
  }

  @Override
  public LaneView getLaneView() {
    return view;
  }

  public void command(T value) {
    state.command(value);
  }
}
