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

import ai.swim.structure.writer.BodyWriter;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.StructuralWriter;
import ai.swim.structure.writer.Writable;

public class ArrayStructuralWritable<E> implements StructuralWritable<E[]> {
  private final Writable<E> writable;

  public ArrayStructuralWritable(Writable<E> writable) {
    this.writable = writable;
  }

  public static StructuralWritable<int[]> forInt() {
    return new IntArrayStructuralWritable();
  }

  public static StructuralWritable<char[]> forChar() {
    return new CharArrayStructuralWritable();
  }

  public static StructuralWritable<long[]> forLong() {
    return new LongArrayStructuralWritable();
  }

  public static StructuralWritable<short[]> forShort() {
    return new ShortArrayStructuralWritable();
  }

  public static StructuralWritable<boolean[]> forBoolean() {
    return new BooleanArrayStructuralWritable();
  }

  public static StructuralWritable<float[]> forFloat() {
    return new FloatArrayStructuralWritable();
  }

  public static StructuralWritable<double[]> forDouble() {
    return new DoubleArrayStructuralWritable();
  }

  @Override
  public <T> T writeInto(E[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (E elem : from) {
      bodyWriter = bodyWriter.writeValue(writable, elem);
    }

    return bodyWriter.done();
  }
}

class IntArrayStructuralWritable implements StructuralWritable<int[]> {
  @Override
  public <T> T writeInto(int[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (int elem : from) {
      bodyWriter = bodyWriter.writeValue(ScalarWriters.INTEGER, elem);
    }

    return bodyWriter.done();
  }
}

class CharArrayStructuralWritable implements StructuralWritable<char[]> {
  @Override
  public <T> T writeInto(char[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (char elem : from) {
      bodyWriter = bodyWriter.writeValue(ScalarWriters.CHARACTER, elem);
    }

    return bodyWriter.done();
  }
}

class LongArrayStructuralWritable implements StructuralWritable<long[]> {
  @Override
  public <T> T writeInto(long[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (long elem : from) {
      bodyWriter = bodyWriter.writeValue(ScalarWriters.LONG, elem);
    }

    return bodyWriter.done();
  }
}

class ShortArrayStructuralWritable implements StructuralWritable<short[]> {
  @Override
  public <T> T writeInto(short[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (short elem : from) {
      bodyWriter = bodyWriter.writeValue(ScalarWriters.SHORT, elem);
    }

    return bodyWriter.done();
  }
}

class BooleanArrayStructuralWritable implements StructuralWritable<boolean[]> {
  @Override
  public <T> T writeInto(boolean[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (boolean elem : from) {
      bodyWriter = bodyWriter.writeValue(ScalarWriters.BOOLEAN, elem);
    }

    return bodyWriter.done();
  }
}

class FloatArrayStructuralWritable implements StructuralWritable<float[]> {
  @Override
  public <T> T writeInto(float[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (float elem : from) {
      bodyWriter = bodyWriter.writeValue(ScalarWriters.FLOAT, elem);
    }

    return bodyWriter.done();
  }
}

class DoubleArrayStructuralWritable implements StructuralWritable<double[]> {
  @Override
  public <T> T writeInto(double[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (double elem : from) {
      bodyWriter = bodyWriter.writeValue(ScalarWriters.DOUBLE, elem);
    }

    return bodyWriter.done();
  }
}