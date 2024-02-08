package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.lang.ffi.NativeLoader;
import ai.swim.server.SwimServerException;
import ai.swim.server.schema.AgentSchema;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import java.io.IOException;
import java.util.List;

public abstract class NativeTest {

  static {
    try {
      NativeLoader.loadLibraries("swim_server");
      NativeLoader.loadLibraries("swim_server_test");
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static native <A extends AbstractAgent> void runNativeAgent(
      byte[] inputs,
      byte[] expectedResponses,
      AgentFactory<A> agentFactory,
      byte[] agentSpec,
      boolean orderedResponses
  );

  public static <A extends AbstractAgent, E> void runAgent(Class<A> agentClass,
      List<TaggedLaneRequest<E>> inputs,
      List<TaggedLaneResponse<E>> outputs,
      Encoder<E> encoder,
      boolean orderedResponses) throws SwimServerException, NoSuchMethodException, IOException {
    AgentSchema<A> agentSchema = AgentSchema.reflectSchema(agentClass);
    AgentFactory<A> agentFactory = AgentFactory.forSchema(agentSchema);

    ByteWriter requestBytes = AgentFixture.encodeIter(inputs, new TaggedLaneRequestEncoder<>(encoder));
    ByteWriter responseBytes = AgentFixture.encodeIter(outputs, new TaggedLaneResponseEncoder<>(encoder));

    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      agentSchema.pack(packer);
      runNativeAgent(
          requestBytes.getArray(),
          responseBytes.getArray(),
          agentFactory,
          packer.toByteArray(),
          orderedResponses);
    }
  }

  public static <A extends AbstractAgent, E> void runAgent(Class<A> agentClass,
      ByteWriter requestBytes,
      ByteWriter responseBytes,
      boolean orderedResponses) throws SwimServerException, NoSuchMethodException, IOException {
    AgentSchema<A> agentSchema = AgentSchema.reflectSchema(agentClass);
    AgentFactory<A> agentFactory = AgentFactory.forSchema(agentSchema);

    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      agentSchema.pack(packer);
      runNativeAgent(
          requestBytes.getArray(),
          responseBytes.getArray(),
          agentFactory,
          packer.toByteArray(),
          orderedResponses);
    }
  }


}
