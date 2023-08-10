package ai.swim.server.agent;

/**
 * Common functionality for an agent.
 */
public interface Agent {
  /**
   * Callback that is invoked when an agent starts.
   */
  void didStart();

  /**
   * Callback that is invoked when an agent stops.
   */
  void didStop();
}
