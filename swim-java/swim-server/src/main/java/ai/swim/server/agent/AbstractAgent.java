package ai.swim.server.agent;

/**
 * Base agent class for implementing Swim Agents.
 */
public abstract class AbstractAgent {
  private final AgentContext context;

  protected AbstractAgent(AgentContext context) {
    this.context = context;
  }

  /**
   * Callback that is invoked when an agent starts.
   */
  public void didStart() {

  }

  /**
   * Callback that is invoked when an agent stops.
   */
  public void didStop() {

  }

  public AgentContext getContext() {
    return context;
  }

}
