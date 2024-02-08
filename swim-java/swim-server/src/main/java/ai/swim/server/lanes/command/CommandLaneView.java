package ai.swim.server.lanes.command;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.lifecycle.OnCommand;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;

public final class CommandLaneView<T> extends LaneView implements CommandLane<T> {
  private final Form<T> form;
  private OnCommand<T> onCommand;
  private CommandLaneModel<T> model;

  public CommandLaneView(Form<T> form) {
    this.form = form;
  }

  @SuppressWarnings("unchecked")
  public void setModel(CommandLaneModel<?> model) {
    this.model = (CommandLaneModel<T>) model;
  }

  @Override
  public Form<T> valueForm() {
    return form;
  }

  @Override
  public CommandLaneView<T> onCommand(OnCommand<T> onCommand) {
    this.onCommand = onCommand;
    return this;
  }

  @Override
  public void command(T value) {
    onCommand(value);
    model.command(value);
  }

  @Override
  public void onCommand(T value) {
    if (onCommand!=null){
      onCommand.onCommand(value);
    }
  }

  @Override
  public LaneModel initLaneModel(StateCollector collector, int laneId) {
    CommandLaneModel<T> model = new CommandLaneModel<>(laneId, this, collector);
    this.model = model;
    return model;
  }

}
