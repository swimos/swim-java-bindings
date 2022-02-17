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
