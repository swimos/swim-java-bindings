// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance  the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// OUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.writer.print;

import ai.swim.structure.writer.*;
import ai.swim.structure.writer.header.WritableHeader;
import ai.swim.structure.writer.print.strategy.PrintStrategy;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;

public class StructurePrinter implements HeaderWriter<String>, BodyWriter<String>, StructuralWriter<String> {
  private final PrintStrategy printStrategy;
  private final SuppressingWriter writer;
  private boolean hasAttr;
  private boolean braceWritten;
  private boolean singleItem;
  private boolean first;
  private AttributePrinter attributePrinter;

  public StructurePrinter(java.io.Writer writer, PrintStrategy printStrategy) {
    this.writer = new SuppressingWriter(writer);
    this.printStrategy = printStrategy;
    this.hasAttr = false;
    this.braceWritten = false;
    this.singleItem = false;
    this.first = true;
    this.attributePrinter = new AttributePrinter(this.writer, printStrategy);
  }

  public static StructurePrinter stdOut(PrintStrategy printStrategy) {
    return new StructurePrinter(new PrintWriter(System.out), printStrategy);
  }

  @Override
  public <V> BodyWriter<String> writeValue(Writable<V> writer, V value) {
    if (hasAttr && !braceWritten) {
      if (singleItem) {
        this.writer.writeUnchecked(' ');
      } else {
        printStrategy.attrPadding().writeInto(this.writer);
        this.writer.writeUnchecked('{');
        braceWritten = true;
      }
    } else if (first) {
      first = false;
    } else {
      this.writer.writeUnchecked(',');
      printStrategy.itemPadding(braceWritten).writeInto(this.writer);
    }

    StructurePrinter valuePrinter = new StructurePrinter(this.writer, printStrategy);
    writer.writeInto(value, valuePrinter);

    return this;
  }

  @Override
  public <K, V> BodyWriter<String> writeSlot(Writable<K> keyWriter, K key, Writable<V> valueWriter, V value) {
    if (first) {
      first = false;
    } else {
      writer.writeUnchecked(',');
      printStrategy.itemPadding(braceWritten).writeInto(writer);
    }

    StructurePrinter keyPrinter = new StructurePrinter(writer, printStrategy);
    keyWriter.writeInto(key, keyPrinter);
    writer.writeUnchecked(':');
    printStrategy.slotPadding().writeInto(writer);

    StructurePrinter valuePrinter = new StructurePrinter(writer, printStrategy);
    valueWriter.writeInto(value, valuePrinter);

    return this;
  }

  @Override
  public String done() {
    if (braceWritten) {
      if (!first) {
        printStrategy.endBlock().writeInto(writer);
      }
      writer.writeUnchecked('}');
    }

    return null;
  }

  @Override
  public HeaderWriter<String> writeExtantAttr(String key) {
    if (hasAttr) {
      printStrategy.attrPadding().writeInto(writer);
    } else {
      hasAttr = true;
    }

    writer.writeUnchecked(String.format("@%s", key));
    return this;
  }

  @Override
  public <V> HeaderWriter<String> writeAttr(String key, Writable<V> valueWriter, V value) {
    writeExtantAttr(key);
    valueWriter.writeInto(value, attributePrinter);

    return this;
  }

  @Override
  public HeaderWriter<String> writeAttr(String key, WritableHeader writable) {
    writeExtantAttr(key);
    writable.writeInto(attributePrinter);

    return this;
  }

  @Override
  public <V> String delegate(Writable<V> valueWriter, V value) {
    return valueWriter.writeInto(value, this);
  }

  @Override
  public BodyWriter<String> completeHeader(int numItems) {
    if (hasAttr) {
      if (numItems > 1) {
        printStrategy.attrPadding().writeInto(writer);
        writer.writeUnchecked('{');
        printStrategy.startBlock(numItems).writeInto(writer);
        braceWritten = true;
      }
    } else {
      writer.writeUnchecked('{');
      printStrategy.startBlock(numItems).writeInto(writer);
      braceWritten = true;
    }

    singleItem = numItems == 1;

    return this;
  }

  @Override
  public String writeExtant() {
    return "";
  }

  @Override
  public String writeInt(int value) {
    return write(Integer.toString(value));
  }

  @Override
  public String writeLong(long value) {
    return write(Long.toString(value));
  }

  @Override
  public String writeFloat(float value) {
    return write(Float.toString(value));
  }

  @Override
  public String writeDouble(double value) {
    return write(Double.toString(value));
  }

  @Override
  public String writeBool(boolean value) {
    return write(Boolean.toString(value));
  }

  @Override
  public String writeBigInt(BigInteger value) {
    return write(value.toString());
  }

  @Override
  public String writeBigDecimal(BigDecimal value) {
    return write(value.toString());
  }

  @Override
  public String writeText(String value) {
    if (StringUtils.isIdentifier(value)) {
      writer.writeUnchecked(value);
      return "";
    } else if (StringUtils.needsEscape(value)) {
      value = StringUtils.escape(value);
    }

    return write(String.format("\"%s\"", value));
  }

  @Override
  public String writeBlob(byte[] value) {
    if (value == null) {
      return "null";
    } else {
      return write("%" + Base64.getEncoder().encodeToString(value));
    }
  }

  @Override
  public HeaderWriter<String> record(int numAttrs) {
    return this;
  }

  private String write(String value) {
    if (value == null) {
      writer.writeUnchecked("null");
    } else {
      if (hasAttr) {
        writer.writeUnchecked(String.format(" %s", value));
      } else {
        writer.writeUnchecked(String.format("%s", value));
      }
    }

    return "";
  }

}
