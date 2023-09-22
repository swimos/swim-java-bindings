package ai.swim.server.lanes.demand;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.lifecycle.OnCue;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;

public final class DemandLaneView<T> extends LaneView implements DemandLane<T> {
  private final Form<T> form;
  private OnCue<T> onCue;
  private DemandLaneModel<T> model;

  public DemandLaneView(Form<T> form) {
    this.form = form;
  }

  @SuppressWarnings({"unchecked", "unused"})
  public void setModel(DemandLaneModel<?> model) {
    this.model = (DemandLaneModel<T>) model;
  }

  @Override
  public Form<T> valueForm() {
    return form;
  }

  @Override
  public DemandLaneView<T> onCue(OnCue<T> onCue) {
    this.onCue = onCue;
    return this;
  }

  @Override
  public T onCue() {
    if (onCue != null) {
      return onCue.onCue();
    } else {
      return null;
    }
  }

  @Override
  public void cue() {
    T value = onCue();
    model.cue(value);
  }

  @Override
  public LaneModel initLaneModel(StateCollector collector, int laneId) {
    DemandLaneModel<T> model = new DemandLaneModel<>(laneId, this, collector);
    this.model = model;
    return model;
  }

}
