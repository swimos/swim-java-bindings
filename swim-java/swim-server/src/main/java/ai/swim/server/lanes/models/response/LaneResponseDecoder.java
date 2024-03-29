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

package ai.swim.server.lanes.models.response;

import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import java.util.UUID;
import static ai.swim.server.lanes.models.response.LaneResponse.EVENT;
import static ai.swim.server.lanes.models.response.LaneResponse.INITIALIZED;
import static ai.swim.server.lanes.models.response.LaneResponse.SYNC;
import static ai.swim.server.lanes.models.response.LaneResponse.SYNC_COMPLETE;
import static ai.swim.server.lanes.models.response.LaneResponse.TAG_LEN;
import static ai.swim.server.lanes.models.response.LaneResponse.UUID_LEN;

/**
 * A decoder for decoding {@link LaneResponse}'s.
 *
 * @param <T> the responses event type.
 */
public class LaneResponseDecoder<T> extends Decoder<LaneResponse<T>> {
  private UUID uuid;
  private State state = State.Header;
  private Decoder<T> delegate;

  public LaneResponseDecoder(Decoder<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Decoder<LaneResponse<T>> decode(ReadBuffer buffer) throws DecoderException {
    while (true) {
      switch (state) {
        case Header:
          if (buffer.remaining() < TAG_LEN) {
            return this;
          } else {
            byte tag = buffer.peekByte();
            switch (tag) {
              case EVENT:
                buffer.advance(1);
                state = State.Standard;
                continue;
              case INITIALIZED:
                buffer.advance(1);
                return Decoder.done(this, LaneResponse.initialized());
              case SYNC:
                if (buffer.remaining() < UUID_LEN + TAG_LEN) {
                  return this;
                } else {
                  buffer.advance(1);
                  long mostSig = buffer.getLong();
                  long leastSig = buffer.getLong();
                  uuid = new UUID(mostSig, leastSig);
                  state = State.Sync;
                  continue;
                }
              case SYNC_COMPLETE:
                if (buffer.remaining() < UUID_LEN + TAG_LEN) {
                  return this;
                } else {
                  buffer.advance(1);
                  long mostSig = buffer.getLong();
                  long leastSig = buffer.getLong();
                  return Decoder.done(this, LaneResponse.synced(new UUID(mostSig, leastSig)));
                }
              default:
                throw new DecoderException("Unknown lane response tag: " + tag);
            }
          }
        case Standard:
          delegate = delegate.decode(buffer);
          if (delegate.isDone()) {
            T body = delegate.bind();
            reset();
            return Decoder.done(this, LaneResponse.event(body));
          } else {
            return this;
          }
        case Sync:
          delegate = delegate.decode(buffer);
          if (delegate.isDone()) {
            T body = delegate.bind();
            UUID remote = uuid;
            reset();
            return Decoder.done(this, LaneResponse.syncEvent(remote, body));
          } else {
            return this;
          }
        default:
          throw new AssertionError();
      }
    }
  }

  @Override
  public Decoder<LaneResponse<T>> reset() {
    state = State.Header;
    delegate = delegate.reset();
    uuid = null;
    return this;
  }

  enum State {
    Header,
    Standard,
    Sync
  }

}
