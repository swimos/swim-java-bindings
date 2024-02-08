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

package ai.swim.server.schema;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static ai.swim.server.lanes.Lanes.valueLane;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaneSchemaTest {

  @Test
  void testReflectAgentSchema() throws SwimServerException {
    AgentSchema<TestAgent> agentSchema = AgentSchema.reflectSchema(TestAgent.class);
    assertEquals(
        agentSchema,
        new AgentSchema<>(
            TestAgent.class,
            "Agent",
            Map.of("testValueLane", new LaneSchema(true, LaneKind.Value, 0))));
  }

  @Test
  void testReflectPlaneSchema() throws SwimServerException {
    PlaneSchema<TestPlane> schema = PlaneSchema.reflectSchema(TestPlane.class);
    Map<Class<? extends AbstractAgent>, String> uriResolver = Map.of(TestAgent.class, "testRoute");
    assertEquals(
        schema,
        new PlaneSchema<>(
            TestPlane.class,
            "testPlane",
            Map.of(
                "testRoute",
                new AgentSchema<>(
                    TestAgent.class,
                    "Agent",
                    Map.of(
                        "testValueLane",
                        new LaneSchema(true, LaneKind.Value, 0)))), uriResolver));
  }

  @SwimAgent("Agent")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane("testValueLane")
    private final ValueLane<Integer> lane = valueLane(Integer.class);

    protected TestAgent(AgentContext context) {
      super(context);
    }
  }

  @SwimPlane("testPlane")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("testRoute")
    TestAgent agent;
  }
}