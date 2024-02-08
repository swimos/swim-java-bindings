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

package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.request.LaneRequestEncoder;
import java.nio.charset.StandardCharsets;

public class TaggedLaneRequestEncoder<T> implements Encoder<TaggedLaneRequest<T>> {
  private final Encoder<LaneRequest<T>> delegate;

  public TaggedLaneRequestEncoder(Encoder<T> delegate) {
    this.delegate = new LaneRequestEncoder<>(delegate);
  }

  @Override
  public void encode(TaggedLaneRequest<T> target, ByteWriter buffer) {
    byte[] bytes = target.getLaneUri().getBytes(StandardCharsets.UTF_8);
    buffer.writeInteger(bytes.length);
    buffer.writeByteArray(bytes);
    buffer.writeByte((byte) (target.isMapLike() ? 1 : 0));
    delegate.encode(target.getRequest(), buffer);
  }
}
