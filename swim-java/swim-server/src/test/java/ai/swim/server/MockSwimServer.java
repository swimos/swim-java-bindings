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
import java.io.IOException;
import java.util.Map;

class MockSwimServer extends AbstractSwimServerBuilder {
  private MockSwimServer(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories,
      PlaneSchema<? extends AbstractPlane> schema) {
    super(agentFactories, schema);
  }

  public static <P extends AbstractPlane> MockSwimServer forPlane(Class<P> planeClass) throws SwimServerException {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);
    return new MockSwimServer(agentFactories, planeSchema);
  }

  private static native void runNative(byte[] config, AbstractSwimServerBuilder plane);

  @Override
  protected long run() throws IOException {
    runNative(schema.bytes(), this);
    return 0;
  }
}
