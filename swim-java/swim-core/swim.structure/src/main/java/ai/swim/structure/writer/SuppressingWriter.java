// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.writer;

import java.io.IOException;
import java.io.Writer;

/**
 * A wrapper around a {@code Writer} that suppresses checked exceptions and instead throws runtime exceptions.
 */
public class SuppressingWriter extends Writer {
  private final Writer delegate;

  public SuppressingWriter(Writer delegate) {
    this.delegate = delegate;
  }

  public void writeUnchecked(int c) {
    try {
      delegate.write(c);
    } catch (IOException e) {
      throw new WriterException(e);
    }
  }

  public void writeUnchecked(String str) {
    try {
      delegate.write(str);
    } catch (IOException e) {
      throw new WriterException(e);
    }
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    delegate.write(cbuf, off, len);
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

}
