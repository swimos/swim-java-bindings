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

package swim.cookbook;

import swim.actor.ActorSpace;
import swim.api.plane.AbstractPlane;
import swim.kernel.Kernel;
import swim.server.ServerLoader;
import swim.structure.Value;

/**
 * A simple Swim plane that the client will communicate with.
 */
public class BasicPlane extends AbstractPlane {

  public static void main(String[] args) {
    // Loads the kernel using the current class loader.
    final Kernel kernel = ServerLoader.loadServer();
    // Loads the defined space from "src/resources/server.recon"; which contains the definitions for the plane, agents,
    // and configuration properties for the transport.
    final ActorSpace space = (ActorSpace) kernel.getSpace("basic");

    kernel.start();
    System.out.println("Running Basic server...");
    kernel.run();

    // Not required but signals the agent to wake up.
    space.command("/unit/foo", "wakeup", Value.absent());
  }

}
