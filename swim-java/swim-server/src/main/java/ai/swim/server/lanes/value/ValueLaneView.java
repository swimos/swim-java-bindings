/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.server.lanes.value;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.lifecycle.OnEvent;
import ai.swim.server.lanes.lifecycle.OnSet;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;

/**
 * {@link ValueLane} and {@link LaneView} implementation.
 *
 * @param <T> the type of the {@link ValueLane}'s event.
 */
public final class ValueLaneView<T> extends LaneView implements ValueLane<T> {
  /**
   * Form for T.
   */
  private final Form<T> form;
  /**
   * {@link OnSet} handler.
   */
  private OnSet<T> onSet;
  /**
   * {@link OnEvent} handler.
   */
  private OnEvent<T> onEvent;
  /**
   * Lane model for dispatching events and retrieving the lane's current state.
   */
  private ValueLaneModel<T> model;

  public ValueLaneView(Form<T> form) {
    this.form = form;
  }

  @SuppressWarnings("unchecked")
  public void setModel(ValueLaneModel<?> model) {
    this.model = (ValueLaneModel<T>) model;
  }

  @Override
  public Form<T> valueForm() {
    return form;
  }

  @Override
  public ValueLaneView<T> onSet(OnSet<T> onSet) {
    this.onSet = onSet;
    return this;
  }

  @Override
  public ValueLaneView<T> onEvent(OnEvent<T> onEvent) {
    this.onEvent = onEvent;
    return this;
  }

  @Override
  public void onSet(T oldValue, T newValue) {
    if (onSet != null) {
      onSet.onSet(oldValue, newValue);
    }
  }

  @Override
  public void onEvent(T value) {
    if (onEvent != null) {
      onEvent.onEvent(value);
    }
  }

  @Override
  public void set(T to) {
    T oldValue = model.get();
    model.set(to);
    onSet(oldValue, to);
  }

  @Override
  public T get() {
    return model.get();
  }

  @Override
  public LaneModel initLaneModel(StateCollector collector, int laneId) {
    ValueLaneModel<T> model = new ValueLaneModel<>(laneId, this, collector);
    this.model = model;
    return model;
  }

}
