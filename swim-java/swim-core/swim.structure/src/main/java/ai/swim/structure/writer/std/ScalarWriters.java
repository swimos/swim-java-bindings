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

package ai.swim.structure.writer.std;

import ai.swim.structure.writer.StructuralWriter;
import ai.swim.structure.writer.Writable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ScalarWriters {

  public final static Writable<String> STRING = new Writable<>() {
    @Override
    public <T> T writeInto(String from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeText(from);
    }
  };

  public final static Writable<Character> CHARACTER = new Writable<>() {
    @Override
    public <T> T writeInto(Character from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeText(from.toString());
    }
  };


  public final static Writable<Integer> INTEGER = new Writable<>() {
    @Override
    public <T> T writeInto(Integer from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeInt(from);
    }
  };

  public final static Writable<Long> LONG = new Writable<>() {
    @Override
    public <T> T writeInto(Long from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeLong(from);
    }
  };

  public final static Writable<Byte> BYTE = new Writable<>() {
    @Override
    public <T> T writeInto(Byte from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeInt(from);
    }
  };

  public final static Writable<Short> SHORT = new Writable<>() {
    @Override
    public <T> T writeInto(Short from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeInt(from);
    }
  };

  public final static Writable<Boolean> BOOLEAN = new Writable<>() {
    @Override
    public <T> T writeInto(Boolean from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeBool(from);
    }
  };

  public final static Writable<Float> FLOAT = new Writable<>() {
    @Override
    public <T> T writeInto(Float from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeFloat(from);
    }
  };

  public final static Writable<Double> DOUBLE = new Writable<>() {
    @Override
    public <T> T writeInto(Double from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeDouble(from);
    }
  };

  public final static Writable<byte[]> PRIMITIVE_BLOB = new Writable<>() {
    @Override
    public <T> T writeInto(byte[] from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeBlob(from);
    }
  };

  public final static Writable<Byte[]> BOXED_BLOB = new Writable<>() {
    @Override
    public <T> T writeInto(Byte[] from, StructuralWriter<T> structuralWriter) {
      byte[] blob = new byte[from.length];

      for (int i = 0; i < from.length; i++) {
        blob[i]=from[i];
      }

      return structuralWriter.writeBlob(blob);
    }
  };

  public final static Writable<BigInteger> BIG_INT = new Writable<>() {
    @Override
    public <T> T writeInto(BigInteger from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeBigInt(from);
    }
  };

  public final static Writable<BigDecimal> BIG_DECIMAL = new Writable<>() {
    @Override
    public <T> T writeInto(BigDecimal from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeBigDecimal(from);
    }
  };

  public final static Writable<AtomicInteger> ATOMIC_INTEGER = new Writable<>() {
    @Override
    public <T> T writeInto(AtomicInteger from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeInt(from.get());
    }
  };

  public final static Writable<AtomicBoolean> ATOMIC_BOOLEAN = new Writable<>() {
    @Override
    public <T> T writeInto(AtomicBoolean from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeBool(from.get());
    }
  };

  public final static Writable<AtomicLong> ATOMIC_LONG = new Writable<>() {
    @Override
    public <T> T writeInto(AtomicLong from, StructuralWriter<T> structuralWriter) {
      return structuralWriter.writeLong(from.get());
    }
  };
}
