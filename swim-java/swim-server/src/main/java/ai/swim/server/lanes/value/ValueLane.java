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

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.lifecycle.OnEvent;
import ai.swim.server.lanes.lifecycle.OnSet;
import ai.swim.structure.Form;

/**
 * Interface for defining Swim Value Lanes.
 *
 * @param <T> the type of the event's.
 */
public interface ValueLane<T> extends Lane {
  /**
   * Returns the {@link Form} for the event type.
   *
   * @return the {@link Form} for the event type
   */
  Form<T> valueForm();

  /**
   * Sets the {@link OnSet} handler.
   *
   * @param onSet the new handler.
   * @return this
   */
  ValueLaneView<T> onSet(OnSet<T> onSet);

  /**
   * Sets the {@link OnEvent} handler.
   *
   * @param onEvent the new handler.
   * @return this
   */
  ValueLaneView<T> onEvent(OnEvent<T> onEvent);

  /**
   * Invokes the {@link OnSet} handler if one has been registered.
   *
   * @param oldValue the previous state.
   * @param newValue the new state.
   */
  void onSet(T oldValue, T newValue);

  /**
   * Invokes the {@link OnEvent} handler if one has been registered.
   *
   * @param value the received event.
   */
  void onEvent(T value);

  /**
   * Sets the state of this {@link ValueLane}.
   *
   * @param to state to set.
   */
  void set(T to);

  /**
   * Returns the current state of this {@link ValueLane}.
   *
   * @return the current state of this {@link ValueLane}.
   */
  T get();
}
