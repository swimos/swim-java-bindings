package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.lang.ffi.NativeLoader;
import ai.swim.server.AbstractSwimServerBuilder;
import ai.swim.server.SwimServerException;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestLaneServer extends AbstractSwimServerBuilder {
  private final ByteWriter responseBytes;
  private final ByteWriter requestBytes;

  static {
    try {
      NativeLoader.loadLibraries("swim_server_test");
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public TestLaneServer(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories,
      PlaneSchema<?> schema,
      ByteWriter requestBytes,
      ByteWriter responseBytes) {
    super(agentFactories, schema);
    this.requestBytes = requestBytes;
    this.responseBytes = responseBytes;
  }

  public static <P extends AbstractPlane, E> TestLaneServer build(Class<P> planeClass,
      List<TaggedLaneRequest<E>> inputs,
      List<TaggedLaneResponse<E>> outputs,
      Encoder<E> encoder) throws SwimServerException {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);

    ByteWriter requestBytes = AgentFixture.encodeIter(inputs, new TaggedLaneRequestEncoder<>(encoder));
    ByteWriter responseBytes = AgentFixture.encodeIter(outputs, new TaggedLaneResponseEncoder<>(encoder));

    return new TestLaneServer(agentFactories, planeSchema, requestBytes, responseBytes);
  }

  public static <P extends AbstractPlane, E> TestLaneServer build(Class<P> planeClass,
      ByteWriter requestBytes,
      ByteWriter responseBytes) throws SwimServerException {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);

    return new TestLaneServer(agentFactories, planeSchema, requestBytes, responseBytes);
  }

  @Override
  protected long run() throws IOException {
    runNativeAgent(requestBytes.getArray(), responseBytes.getArray(), this, schema.bytes());
    return 0;
  }

  private static native void runNativeAgent(byte[] inputs,
      byte[] expectedResponses,
      AbstractSwimServerBuilder server,
      byte[] planeSpec);
}
