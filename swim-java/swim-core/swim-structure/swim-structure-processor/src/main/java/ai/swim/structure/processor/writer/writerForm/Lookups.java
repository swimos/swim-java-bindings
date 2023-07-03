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

package ai.swim.structure.processor.writer.writerForm;

public class Lookups {

  public static final String STD_PACKAGE = "ai.swim.structure.writer.std";
  public static final String LIST_WRITER_CLASS = "ListStructuralWritable";
  public static final String ARRAY_WRITER_CLASS = "ArrayStructuralWritable";
  public static final String MAP_WRITER_CLASS = "MapStructuralWritable";
  public static final String WRITABLE_CLASS = "ai.swim.structure.writer.Writable";
  public static final String WRITABLE_WRITE_INTO = "writeInto";
  public static final String STRUCTURAL_WRITER_CLASS = "ai.swim.structure.writer.StructuralWriter";
  public static final String WRITER_PROXY = "ai.swim.structure.writer.proxy.WriterProxy";
  public static final String WRITER_EXCEPTION = "ai.swim.structure.writer.WriterException";
  public static final String HEADER_WRITER = "ai.swim.structure.writer.HeaderWriter";
  public static final String BODY_WRITER = "ai.swim.structure.writer.BodyWriter";
  public static final String HEADER_NO_SLOTS = "ai.swim.structure.writer.header.Header.NoSlots";

  private Lookups() {
    throw new AssertionError();
  }
}
