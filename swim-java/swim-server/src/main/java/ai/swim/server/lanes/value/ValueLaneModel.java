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

/**
 * {@link ValueLane} {@link LaneModel} implementation.
 *
 * @param <T> the type of the {@link ValueLane}'s event.
 */
public final class ValueLaneModel<T> extends LaneModel {
  /**
   * {@link LaneView} into the lane.
   */
  private final ValueLaneView<T> view;
  /**
   * Form for T.
   */
  private final Form<T> form;
  /**
   * The state of the lane.
   */
  private final ValueState<T> state;

  public ValueLaneModel(int laneId, ValueLaneView<T> view, StateCollector collector) {
    this.view = view;
    this.form = view.valueForm();
    this.state = new ValueState<>(laneId, form, collector);
  }

  @Override
  public void dispatch(ReadBuffer buffer) {
    Parser<T> parser = new FormParser<>(form.reset());
    parser = parser.feed(Input.readBuffer(buffer));

    if (parser.isDone()) {
      T newValue = parser.bind();
      T oldValue = state.set(newValue);

      view.onEvent(newValue);
      view.onSet(oldValue, newValue);
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
    Parser<T> parser = new FormParser<>(form.reset());
    parser = parser.feed(Input.readBuffer(buffer));
    if (parser.isDone()) {
      state.set(parser.bind());
    } else if (parser.isError()) {
      ParserError<T> error = (ParserError<T>) parser;
      throw new RecognizerException(String.format("%s at: %s", error.cause(), error.location()));
    } else {
      throw new RecognizerException("Unconsumed input");
    }
  }

  @Override
  public LaneView getLaneView() {
    return view;
  }

  public T get() {
    return state.get();
  }

  public void set(T to) {
    state.set(to);
  }

}
