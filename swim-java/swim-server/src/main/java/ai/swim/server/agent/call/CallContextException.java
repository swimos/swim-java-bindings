package ai.swim.server.agent.call;

/**
 * Exception thrown when a thread attempts to illegally access the Swim runtime.
 */
public class CallContextException extends RuntimeException {
  public CallContextException(String message) {
    super(message);
  }

  public static CallContextException illegalAccess(CallContext.ThreadInfo state) {
    String msg = String.format(
        "Attempted to access Swim runtime outside of context; all events must be fired from the Swim runtime. Thread: '%s' is not registered for access",
        state);
    return new CallContextException(msg);
  }
}
