package ai.swim.server.lanes.supply;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;

public final class SupplyLaneView<T> extends LaneView implements SupplyLane<T> {
  private final Form<T> form;
  private SupplyLaneModel<T> model;

  public SupplyLaneView(Form<T> form) {
    this.form = form;
  }

  @SuppressWarnings({"unchecked", "unused"})
  public void setModel(SupplyLaneModel<?> model) {
    this.model = (SupplyLaneModel<T>) model;
  }

  @Override
  public Form<T> valueForm() {
    return form;
  }

  @Override
  public void push(T value) {
    model.push(value);
  }

  @Override
  public LaneModel initLaneModel(StateCollector collector, int laneId) {
    SupplyLaneModel<T> model = new SupplyLaneModel<>(laneId, this, collector);
    this.model = model;
    return model;
  }

}
