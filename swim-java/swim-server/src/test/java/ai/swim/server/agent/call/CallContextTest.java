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

package ai.swim.server.agent.call;

import ai.swim.codec.decoder.DecoderException;
import ai.swim.concurrent.Trigger;
import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.agent.AgentNode;
import ai.swim.server.agent.AgentView;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneModel;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.schema.AgentSchema;
import ai.swim.structure.Form;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static ai.swim.server.lanes.Lanes.valueLane;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class CallContextTest {

  @Test
  void laneAccess() throws InterruptedException, DecoderException {
    StateCollector stateCollector = new StateCollector();
    Trigger trigger = new Trigger();
    ValueLaneView<Integer> laneView = new ValueLaneView<>(Form.forClass(Integer.class));

    laneView.onSet((oldValue, newValue) -> {
      // OnSet is invoked by the node and is valid access.
      Thread thread = new Thread(() -> {
        // Invalid access here as the thread has not entered a call context.

        trigger.trigger();
        assertThrows(CallContextException.class, () -> laneView.set(20));
        fail("Expected a CallContextException to be thrown");
      });
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    ValueLaneModel<Integer> model = new ValueLaneModel<>(0, laneView, stateCollector);
    laneView.setModel(model);

    AgentNode node = new AgentNode(stateCollector, Map.of(0, model), Map.of("test", 0));

    byte[] msg = "13".getBytes(StandardCharsets.UTF_8);
    node.dispatch(0, ByteBuffer.wrap(msg));

    trigger.awaitTrigger(5, TimeUnit.SECONDS);
  }

  @Test
  void agentContext() throws SwimServerException, NoSuchMethodException, DecoderException {
    AgentSchema<TestAgent> agentSchema = AgentSchema.reflectSchema(TestAgent.class);
    AgentFactory<TestAgent> agentFactory = AgentFactory.forSchema(agentSchema);
    AgentView view = agentFactory.newInstance(0);

    byte[] msg = "13".getBytes(StandardCharsets.UTF_8);
    view.dispatch(0, ByteBuffer.wrap(msg), msg.length);
  }

  @SwimAgent("agent")
  private static class TestAgent extends AbstractAgent {
    protected TestAgent(AgentContext context) {
      super(context);
    }

    private void run() {
      Thread thread = new Thread(() -> assertThrows(CallContextException.class, () -> {
        valueLane.set(20);
        fail("Expected a CallContextException to be thrown");
      }));

      thread.start();

      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }    @SwimLane
    private final ValueLane<Integer> valueLane = valueLane(Integer.class).onSet(((oldValue, newValue) -> {
      run();
    }));


  }

}