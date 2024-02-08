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
import java.util.Objects;
import java.util.UUID;
import static ai.swim.server.lanes.models.response.LaneResponse.TAG_LEN;
import static ai.swim.server.lanes.models.response.LaneResponse.UUID_LEN;

public abstract class LaneRequest<T> {

  public static final byte COMMAND = 0;
  public static final byte SYNC = 1;
  public static final byte SYNC_COMPLETE = 2;

  public static <T> LaneRequest<T> initComplete() {
    return new InitComplete<>();
  }

  public static <T> LaneRequest<T> command(T event) {
    return new Command<>(event);
  }

  public static <T> LaneRequest<T> sync(UUID remote) {
    return new Sync<>(remote);
  }

  public abstract void encode(Encoder<T> encoder, ByteWriter buffer);

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();

  private static class InitComplete<T> extends LaneRequest<T> {
    @Override
    public void encode(Encoder<T> encoder, ByteWriter buffer) {
      buffer.writeByte(SYNC);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      return o != null && getClass() == o.getClass();
    }

    @Override
    public String toString() {
      return "InitComplete{}";
    }
  }

  private static class Command<T> extends LaneRequest<T> {
    private final T event;

    private Command(T event) {
      this.event = event;
    }

    @Override
    public void encode(Encoder<T> encoder, ByteWriter buffer) {
      buffer.writeByte(COMMAND);
      encoder.encode(event, buffer);
    }

    @Override
    public String toString() {
      return "Command{" +
          "event=" + event +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Command<?> command = (Command<?>) o;
      return Objects.equals(event, command.event);
    }

    @Override
    public int hashCode() {
      return Objects.hash(event);
    }
  }

  private static class Sync<T> extends LaneRequest<T> {
    private final UUID remote;

    private Sync(UUID remote) {
      this.remote = remote;
    }

    @Override
    public void encode(Encoder<T> encoder, ByteWriter buffer) {
      buffer.reserve(TAG_LEN + UUID_LEN);
      buffer.writeByte(SYNC);
      buffer.writeLong(remote.getMostSignificantBits());
      buffer.writeLong(remote.getLeastSignificantBits());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Sync<?> sync = (Sync<?>) o;
      return Objects.equals(remote, sync.remote);
    }

    @Override
    public int hashCode() {
      return Objects.hash(remote);
    }

    @Override
    public String toString() {
      return "Sync{" +
          "remote=" + remote +
          '}';
    }
  }
}
