package ai.swim.server.lanes.command;

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.lifecycle.OnCommand;
import ai.swim.structure.Form;

/**
 * Swim Command Lane interface.
 *
 * @param <T> the key type this lane produces.
 */
public interface CommandLane<T> extends Lane {
  /**
   * Returns the {@link Form} associated with the value type.
   */
  Form<T> valueForm();

  /**
   * Sets the {@link OnCommand} that will be invoked when the lane receives a command.
   */
  CommandLaneView<T> onCommand(OnCommand<T> onCommand);

  /**
   * Sends a command to the lane
   *
   * @param value the command
   */
  void command(T value);

  /**
   * Invokes the registered {@link OnCommand} callback.
   */
  void onCommand(T value);
}
