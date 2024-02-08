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

package ai.swim.codec.decoder;

/**
 * An exception thrown when it was not possible to decode the buffer.
 */
public class DecoderException extends Exception {
  public DecoderException() {
  }

  public DecoderException(Throwable cause) {
    super(cause);
  }

  public DecoderException(String message) {
    super(message);
  }

  public DecoderException(String message, Throwable cause) {
    super(message, cause);
  }
}
