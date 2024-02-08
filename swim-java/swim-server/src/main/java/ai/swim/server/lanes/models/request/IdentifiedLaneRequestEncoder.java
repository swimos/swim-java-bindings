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

package ai.swim.server.lanes.models.request;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;

public class IdentifiedLaneRequestEncoder<T> implements Encoder<IdentifiedLaneRequest<T>> {
  private final Encoder<LaneRequest<T>> delegate;

  public IdentifiedLaneRequestEncoder(Encoder<T> delegate) {
    this.delegate = new LaneRequestEncoder<>(delegate);
  }

  @Override
  public void encode(IdentifiedLaneRequest<T> target, ByteWriter buffer) {
    buffer.writeInteger(target.getLaneId());
    delegate.encode(target.getLaneRequest(), buffer);
  }
}
