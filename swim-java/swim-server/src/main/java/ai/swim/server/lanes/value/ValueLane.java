package ai.swim.server.lanes.value;

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.lifecycle.OnEvent;
import ai.swim.server.lanes.lifecycle.OnSet;
import ai.swim.structure.Form;

public interface ValueLane<T> extends Lane {
  Form<T> valueForm();

  ValueLaneView<T> onSet(OnSet<T> onSet);

  ValueLaneView<T> onEvent(OnEvent<T> onEvent);

  void onSet(T oldValue, T newValue);

  void onEvent(T value);

  void set(T to);

  T get();
}
