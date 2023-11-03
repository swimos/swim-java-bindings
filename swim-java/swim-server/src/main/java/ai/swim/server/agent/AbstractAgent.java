package ai.swim.server.agent;

import ai.swim.server.agent.call.CallContext;
import ai.swim.server.agent.call.CallContextException;

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

  /**
   * Returns the {@link AgentContext} associated with a {@link AbstractAgent}.
   *
   * @throws CallContextException if called from outside the context of the Swim runtime. This can happen if a thread
   *                              has been spawned that was not started by the Swim runtime, and it attempts to access
   *                              the context.
   */
  public AgentContext getContext() {
    CallContext.check();
    return context;
  }

}