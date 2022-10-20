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

package ai.swim.structure;

import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.print.StructurePrinter;
import ai.swim.structure.writer.print.strategy.PrettyPrintStrategy;
import ai.swim.structure.writer.print.strategy.PrintStrategy;

import java.io.Writer;

public class Recon {
  public static <V, T extends Writable<V>> void printRecon(Writer writer, T writable, V value) {
    Recon.print(writer, writable, value, PrintStrategy.STANDARD);
  }

  public static <V, T extends Writable<V>> void printReconCompact(Writer writer, T writable, V value) {
    Recon.print(writer, writable, value, PrintStrategy.COMPACT);
  }

  public static <V, T extends Writable<V>> void printReconPretty(Writer writer, T writable, V value) {
    Recon.print(writer, writable, value, new PrettyPrintStrategy());
  }

  public static <V, T extends Writable<V>> void print(Writer writer, T writable, V value, PrintStrategy strategy) {
    writable.writeInto(value, new StructurePrinter(writer, strategy));
  }
}
