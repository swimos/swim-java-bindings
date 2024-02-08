package ai.swim.server.lanes.demand;

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.lifecycle.OnCue;
import ai.swim.structure.Form;

/**
 * Swim Demand Lane interface.
 *
 * @param <T> the type this lane produces.
 */
public interface DemandLane<T> extends Lane {
  /**
   * Returns the {@link Form} associated with the target type.
   */
  Form<T> valueForm();

  /**
   * Sets the {@link OnCue} that will be invoked when a value is to be produced.
   */
  DemandLaneView<T> onCue(OnCue<T> onCue);

  /**
   * Invokes the registered {@link OnCue} callback
   */
  T onCue();

  /**
   * Cues a value.
   */
  void cue();

}
