package ai.swim.server.agent;

/**
 * JNI function table for {@link AgentContext}.
 */
// Localised here to avoid the AgentContext class containing too much noise.
public class AgentContextFunctionTable {
  /**
   * Opens a new lane on the agent.
   *
   * @param handlePtr to the JavaAgentContext struct.
   * @param laneUri   of the lane to open.
   * @param layout    encoded {@link ai.swim.server.schema.LaneSchema}
   */
  static native void openLane(long handlePtr, String laneUri, byte[] layout);

  /**
   * Drops the JavaAgentContext struct.
   * <p>
   * This should never need to be manually invoked. It *should* only ever be invoked by the corresponding {@link ai.swim.lang.ffi.AtomicDestructor}.
   *
   * @param handlePtr to the JavaAgentContext struct.
   */
  static native void dropHandle(long handlePtr);

  static native void suspendTask(long handlePtr, long resumeAfterSeconds, int resumeAfterNanos, long idMsb, long idLsb);

  static native void scheduleTaskIndefinitely(long handlePtr,
      long intervalSeconds,
      int intervalNanos,
      long idMsb,
      long idLsb);

  static native void repeatTask(long handlePtr,
      int runCount,
      long intervalSeconds,
      int intervalNanos,
      long idMsb,
      long idLsb);

  public static native void cancelTask(long ptr, long mostSignificantBits, long leastSignificantBits);
}
