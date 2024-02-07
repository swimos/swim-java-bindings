package ai.swim.server.agent;

/**
 * Exception type thrown by an agent if there is an issue decoding a message.
 * <p>
 * Throwing this exception will cause the agent to shut down.
 */
class AgentException extends RuntimeException {
  public AgentException() {
    super();
  }

  public AgentException(String message) {
    super(message);
  }

  public AgentException(String message, Throwable cause) {
    super(message, cause);
  }

  public AgentException(Throwable cause) {
    super(cause);
  }

  protected AgentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
