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

import ai.swim.structure.writer.BodyWriter;
import ai.swim.structure.writer.HeaderWriter;
import ai.swim.structure.writer.StringUtils;
import ai.swim.structure.writer.StructuralWriter;
import ai.swim.structure.writer.SuppressingWriter;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.header.WritableHeader;
import ai.swim.structure.writer.print.strategy.PrintStrategy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;

public class AttributePrinter implements HeaderWriter<String>, BodyWriter<String>, StructuralWriter<String> {
  private final PrintStrategy printStrategy;
  private final SuppressingWriter writer;
  private boolean hasAttr;
  private boolean braceWritten;
  private boolean singleItem;
  private boolean first;
  private boolean delegated;

  public AttributePrinter(SuppressingWriter writer, PrintStrategy printStrategy) {
    this.writer = writer;
    this.printStrategy = printStrategy;
    this.hasAttr = false;
    this.braceWritten = false;
    this.singleItem = false;
    this.first = true;
    this.delegated = false;
  }

  @Override
  public <V> BodyWriter<String> writeValue(Writable<V> writer, V value) {
    if (!braceWritten && !hasAttr && singleItem) {
      this.writer.writeUnchecked('{');
      printStrategy.startBlock(1).writeInto(this.writer);
      braceWritten = true;
    }

    if (first) {
      first = false;
    } else {
      this.writer.writeUnchecked(',');
      printStrategy.itemPadding(braceWritten).writeInto(this.writer);
    }

    StructurePrinter printer = new StructurePrinter(this.writer, printStrategy);
    writer.writeInto(value, printer);

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

    writer.writeUnchecked(')');
    return "";
  }

  @Override
  public <V> HeaderWriter<String> writeAttr(String key, Writable<V> valueWriter, V value) {
    if (hasAttr) {
      printStrategy.attrPadding().writeInto(writer);
    } else {
      hasAttr = true;
    }

    writer.writeUnchecked(String.format("@%s", key));

    AttributePrinter attrPrinter = new AttributePrinter(writer, printStrategy);
    valueWriter.writeInto(value, attrPrinter);

    return this;
  }

  @Override
  public HeaderWriter<String> writeAttr(String key, WritableHeader writable) {
    if (hasAttr) {
      printStrategy.attrPadding().writeInto(writer);
    } else {
      hasAttr = true;
    }

    writer.writeUnchecked(String.format("@%s", key));
    writable.writeInto(this);

    return this;
  }

  @Override
  public <V> String delegate(Writable<V> valueWriter, V value) {
    delegated = true;
    return valueWriter.writeInto(value, this);
  }

  @Override
  public BodyWriter<String> completeHeader(int numItems) {
    singleItem = numItems == 1;
    if (hasAttr) {
      if (numItems != 0) {
        if (numItems == 1) {
          writer.writeUnchecked(' ');
        } else {
          printStrategy.attrPadding().writeInto(writer);
          writer.writeUnchecked('{');
          printStrategy.startBlock(numItems).writeInto(writer);
          braceWritten = true;
        }
      }
    } else if (numItems == 0) {
      writer.writeUnchecked('{');
      printStrategy.startBlock(numItems).writeInto(writer);
      braceWritten = true;
    }

    return this;
  }

  @Override
  public HeaderWriter<String> writeExtantAttr(String key) {
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
    if (delegated) {
      if (hasAttr) {
        writer.writeUnchecked(" ");
      }
    } else {
      writer.writeUnchecked(String.format("(%s", printStrategy.attrBodyPadding()));
    }

    if (StringUtils.isIdentifier(value)) {
      writer.writeUnchecked(value);
    } else if (StringUtils.needsEscape(value)) {
      value = StringUtils.escape(value);
    }

    writer.writeUnchecked(String.format("\"%s\"", value));

    return "";
  }

  @Override
  public String writeBlob(byte[] value) {
    if (value == null) {
      return "null";
    } else {
      return "%" + Base64.getEncoder().encodeToString(value);
    }
  }

  @Override
  public HeaderWriter<String> record(int numAttrs) {
    writer.writeUnchecked(String.format("(%s", printStrategy.attrBodyPadding()));
    return this;
  }

  private String write(String value) {
    if (value == null) {
      writer.writeUnchecked("null");
    } else {
      if (delegated) {
        if (hasAttr) {
          writer.writeUnchecked(String.format(" %s%s)", value, printStrategy.attrBodyPadding()));
        } else {
          writer.writeUnchecked(String.format("%s%s)", value, printStrategy.attrBodyPadding()));
        }
      } else {
        writer.writeUnchecked(String.format("(%s%s%s)", printStrategy.attrBodyPadding(), value, printStrategy.attrBodyPadding()));
      }
    }

    return "";
  }
}
