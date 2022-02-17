// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.io;//package ai.swim.io;
//
//import ai.swim.sync.RAtomicBool;
//import ai.swim.sync.RAtomicU64;
//import java.nio.ByteBuffer;
//
//public class ByteWriter {
//
//  private final RAtomicU64 readIndex;
//  private final RAtomicU64 writeIndex;
//  private final ByteBuffer data;
//  private final RAtomicBool channelActive;
//
//  private final long readPtr;
//
//  private ByteWriter(RAtomicU64 readIndex, RAtomicU64 writeIndex, ByteBuffer data, RAtomicBool channelActive, long readPtr) {
//    this.readIndex = readIndex;
//    this.writeIndex = writeIndex;
//    this.data = data;
//    this.channelActive = channelActive;
//    this.readPtr = readPtr;
//  }
//
//  public static ByteWriter create(int capacity) {
//    RAtomicU64 readIndex = RAtomicU64.create(0);
//    RAtomicU64 writeIndex = RAtomicU64.create(0);
//    ByteBuffer data = ByteBuffer.allocateDirect(capacity);
//    RAtomicBool channelActive = RAtomicBool.create(true);
//
//    return ByteWriter.createNative(readIndex, writeIndex, data, channelActive);
//  }
//
////  private static native ByteWriter createNative(RAtomicU64 readIndex, RAtomicU64 writeIndex, ByteBuffer data, RAtomicBool channelActive);
//
//}
