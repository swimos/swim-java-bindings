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

package ai.swim.server;

import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.util.Map;

/**
 * Base class for building and running a new Swim Server.
 */
public class SwimServer extends AbstractSwimServerBuilder {
  private SwimServer(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories, PlaneSchema<?> schema) {
    super(agentFactories, schema);
  }

  /**
   * Builds a Swim Server instance for the provided plane.
   *
   * @param planeClass to reflect and build the Swim Server from
   * @param <P>        the type of the plane
   * @return a new Swim Server instance
   * @throws SwimServerException if the provided class is not well-defined
   */
  public static <P extends AbstractPlane> SwimServer forPlane(Class<P> planeClass) throws SwimServerException {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);
    return new SwimServer(agentFactories, planeSchema);
  }

  private static native long runSwimServer();

  @Override
  protected long run() {
    return runSwimServer();
  }
}
