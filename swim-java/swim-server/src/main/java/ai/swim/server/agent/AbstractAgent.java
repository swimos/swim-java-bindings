/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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