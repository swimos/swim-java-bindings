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

package ai.swim.server.agent.lanes.models.response;

import ai.swim.codec.Size;
import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.lanes.models.response.LaneResponse;

/**
 * A decoder for decoding {@link IdentifiedLaneResponse}'s.
 *
 * @param <T> the lane response's event type.
 */
public class IdentifiedLaneResponseDecoder<T> extends Decoder<IdentifiedLaneResponse<T>> {
  private Decoder<LaneResponse<T>> delegate;
  private State state;
  private int laneId;
  public IdentifiedLaneResponseDecoder(Decoder<LaneResponse<T>> delegate) {
    this.delegate = delegate;
    this.state = State.LaneId;
  }

  @Override
  public Decoder<IdentifiedLaneResponse<T>> decode(ReadBuffer buffer) throws DecoderException {
    while (true) {
      switch (state) {
        case LaneId:
          if (buffer.remaining() >= Size.INT) {
            laneId = buffer.getInteger();
            // Discard the length as it is only used when writing to Rust.
            buffer.getInteger();
            state = State.Delegated;
            break;
          } else {
            return this;
          }
        case Delegated:
          delegate = delegate.decode(buffer);
          if (delegate.isDone()) {
            return Decoder.done(this, new IdentifiedLaneResponse<>(laneId, delegate.bind()));
          } else {
            return this;
          }
      }
    }
  }

  @Override
  public Decoder<IdentifiedLaneResponse<T>> reset() {
    return new IdentifiedLaneResponseDecoder<>(delegate.reset());
  }

  enum State {
    LaneId,
    Delegated
  }
}
