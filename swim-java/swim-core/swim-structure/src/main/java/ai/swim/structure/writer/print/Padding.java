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

package ai.swim.structure.writer.print;

import ai.swim.structure.writer.WriterException;
import java.io.IOException;
import java.io.Writer;

public abstract class Padding {
  public abstract void writeInto(Writer writer);

  @Override
  public abstract String toString();

  public static class Simple extends Padding {
    public static final Padding SINGLE_SPACE = new Simple(" ");
    public static final Padding NO_SPACE = new Simple("");
    private final String padding;

    Simple(String s) {
      this.padding = s;
    }

    @Override
    public void writeInto(Writer writer) {
      try {
        writer.write(padding);
      } catch (IOException e) {
        throw new WriterException(e);
      }
    }

    @Override
    public String toString() {
      return padding;
    }
  }

  public static class Complex extends Padding {
    private final String prefix;
    private final String block;
    private final int level;

    public Complex(String prefix, String block, int level) {
      this.prefix = prefix;
      this.block = block;
      this.level = level;
    }

    @Override
    public void writeInto(Writer writer) {
      try {
        writer.write(prefix);
        for (int i = 0; i < level; i++) {
          writer.write(block);
        }
      } catch (IOException e) {
        throw new WriterException(e);
      }
    }

    @Override
    public String toString() {
      return prefix + block.repeat(level);
    }
  }
}
