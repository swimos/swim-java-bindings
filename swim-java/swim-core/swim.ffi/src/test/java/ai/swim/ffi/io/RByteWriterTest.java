// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.ffi.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ai.swim.ffi.JniRunner;
import org.junit.jupiter.api.Test;

class RByteWriterTest extends JniRunner {

  @Test
  void tryWrite() {
    for (int i = 0; i < 1000; i++) {
      RByteWriter writer = RByteWriter.create(8);

      long now = System.nanoTime();
      int count = writer.tryWrite(new byte[] {1, 2, 3, 4, 5, 6});
    }
  }

  @Test
  void write() {
    Random random = new Random();
    List<Long> avg = new ArrayList<>(1000);

    for (int a = 0; a < 5; a++) {
      for (int i = 0; i < 1000; i++) {
        RByteWriter writer = RByteWriter.create(1024);
        long start = System.nanoTime();

        for (int j = 1; j <= 1000; j++) {
          byte[] bytes = new byte[128];
          random.nextBytes(bytes);
          writer.write(bytes);
        }

        avg.add(System.nanoTime() - start);
      }
    }

    System.out.println(avg.stream()
        .mapToDouble(e -> e)
        .average().getAsDouble() + "ms");
  }

}
