// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.client.downlink.map;

import ai.swim.client.SwimClientException;
import ai.swim.client.downlink.FfiTest;
import ai.swim.client.downlink.TriConsumer;
import ai.swim.client.lifecycle.OnLinked;
import ai.swim.client.lifecycle.OnUnlinked;
import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.concurrent.Trigger;
import ai.swim.structure.Form;
import ai.swim.structure.FormParser;
import ai.swim.structure.Recon;
import ai.swim.structure.annotations.AutoForm;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MapDownlinkTest extends FfiTest {

  private static native void callbackTest(
      OnLinked onLinked,
      Routine onSynced,
      TriConsumer<ByteBuffer, ByteBuffer, Boolean> onUpdate,
      BiConsumer<ByteBuffer, Boolean> onRemove,
      Consumer<Boolean> onClear,
      OnUnlinked onUnlinked,
      BiConsumer<Integer, Boolean> take,
      BiConsumer<Integer, Boolean> drop
  ) throws SwimClientException;

  private static native long lifecycleTest(
      Trigger lock,
      String input,
      String host,
      String node,
      String lane,
      OnLinked onLinked,
      Routine onSynced,
      TriConsumer<ByteBuffer, ByteBuffer, Boolean> onUpdate,
      BiConsumer<ByteBuffer, Boolean> onRemove,
      Consumer<Boolean> onClear,
      OnUnlinked onUnlinked,
      BiConsumer<Integer, Boolean> take,
      BiConsumer<Integer, Boolean> drop
  ) throws SwimClientException;

  String parseString(ByteBuffer buffer) {
    Input input = Input.byteBuffer(buffer);
    Parser<String> parser = new FormParser<>(String.class).feed(input);
    assertTrue(parser.isDone());
    return parser.bind();
  }

  @Test
  void callbackTest() throws SwimClientException {
    AtomicInteger linkedInvoked = new AtomicInteger(0);
    AtomicInteger syncedInvoked = new AtomicInteger(0);
    AtomicInteger updateInvoked = new AtomicInteger(0);
    AtomicInteger removeInvoked = new AtomicInteger(0);
    AtomicInteger clearInvoked = new AtomicInteger(0);
    AtomicInteger unlinkedInvoked = new AtomicInteger(0);
    AtomicInteger takeInvoked = new AtomicInteger(0);
    AtomicInteger dropInvoked = new AtomicInteger(0);

    HashMap<String, String> view = new HashMap<>();

    callbackTest(
        () -> linkedInvoked.addAndGet(1),
        () -> {
          syncedInvoked.addAndGet(1);
          assertEquals(Map.of("key1", "value1", "key2", "value2"), view);
        },
        (key, value, dispatch) -> {
          assertFalse(dispatch);
          view.put(parseString(key), parseString(value));
          updateInvoked.addAndGet(1);
        },
        (key, dispatch) -> {
          assertTrue(dispatch);
          assertEquals("key2", parseString(key));
          removeInvoked.addAndGet(1);
        },
        (dispatch) -> {
          assertTrue(dispatch);
          clearInvoked.addAndGet(1);
          view.clear();
        },
        () -> unlinkedInvoked.addAndGet(1),
        (n, dispatch) -> {
          assertTrue(dispatch);
          assertEquals(5, n);
          takeInvoked.addAndGet(1);
        },
        (n, dispatch) -> {
          assertTrue(dispatch);
          assertEquals(3, n);
          dropInvoked.addAndGet(1);
        }
    );

    assertEquals(1, linkedInvoked.get());
    assertEquals(1, syncedInvoked.get());
    assertEquals(2, updateInvoked.get());
    assertEquals(1, removeInvoked.get());
    assertEquals(1, clearInvoked.get());
    assertEquals(1, unlinkedInvoked.get());
    assertEquals(1, takeInvoked.get());
    assertEquals(1, dropInvoked.get());
    assertTrue(view.isEmpty());
  }

  @Test
  void simpleOpen() throws InterruptedException, SwimClientException {
    Trigger lock = new Trigger();
    long ptr = lifecycleTest(
        lock,
        "",
        "host",
        "node",
        "lane",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    awaitTrigger(lock, 5, "downlink");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleLinked() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@linked(node:node, lane:lane)",
        "host",
        "node",
        "lane",
        invoked::countDown,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleSynced() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@linked(node:node, lane:lane)\n@synced(node:node, lane:lane)",
        "host",
        "node",
        "lane",
        null,
        invoked::countDown,
        null,
        null,
        null,
        null,
        null,
        null
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleUpdate() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@linked(node:node, lane:lane)\n@event(node:node, lane:lane)@update(key:key)1",
        "host",
        "node",
        "lane",
        null,
        null,
        (a, b, c) -> invoked.countDown(),
        null,
        null,
        null,
        null,
        null
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleRemove() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@linked(node:node, lane:lane)\n@event(node:node, lane:lane)@remove(key:key)",
        "host",
        "node",
        "lane",
        null,
        null,
        null,
        (a, b) -> invoked.countDown(),
        null,
        null,
        null,
        null
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleClear() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@linked(node:node, lane:lane)\n@event(node:node, lane:lane)@clear",
        "host",
        "node",
        "lane",
        null,
        null,
        null,
        null,
        (a) -> invoked.countDown(),
        null,
        null,
        null
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleUnlinked() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@unlinked(node:node, lane:lane)",
        "host",
        "node",
        "lane",
        null,
        null,
        null,
        null,
        null,
        invoked::countDown,
        null,
        null
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleTake() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@linked(node:node, lane:lane)\n@event(node:node, lane:lane)@take(5)",
        "host",
        "node",
        "lane",
        null,
        null,
        null,
        null,
        null,
        null,
        (a, b) -> invoked.countDown(),
        null
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  @Test
  void lifecycleDrop() throws SwimClientException, InterruptedException {
    Trigger lock = new Trigger();
    CountDownLatch invoked = new CountDownLatch(1);

    long ptr = lifecycleTest(
        lock,
        "@linked(node:node, lane:lane)\n@event(node:node, lane:lane)@drop(5)",
        "host",
        "node",
        "lane",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        (a, b) -> invoked.countDown()
    );

    awaitTrigger(lock, 5, "downlink");
    awaitLatch(invoked, 5, "lifecycle");
    dropRuntime(ptr);
  }

  <K, V> void runTestOk(Class<K> keyClass, Class<V> valueClass, int removeCount, ConcurrentLinkedDeque<Update<K, V>> syncEvents, ConcurrentLinkedDeque<MapMessage> events, Map<K, V> finalState) throws InterruptedException, SwimClientException {
    StringBuilder input = new StringBuilder();
    input.append("@linked(node:node,lane:lane)\n");

    for (Update<K, V> event : syncEvents) {
      input.append(String.format("@event(node:node,lane:lane){%s}\n", Recon.toStringCompact(event)));
    }

    input.append("@synced(node:node,lane:lane)\n");

    int updateCount = 0;
    int clearCount = 0;

    for (MapMessage event : events) {
      if (event.isUpdate()) {
        updateCount += 1;
      } else if (event.isClear()) {
        clearCount += 1;
      }

      input.append(String.format("@event(node:node,lane:lane){%s}\n", Recon.toStringCompact(event)));
    }

    input.append("@unlinked(node:node,lane:lane)\n");

    Trigger lock = new Trigger();
    AtomicReference<LinkState> linkState = new AtomicReference<>(LinkState.Init);

    CountDownLatch linked = new CountDownLatch(1);
    CountDownLatch synced = new CountDownLatch(1);
    CountDownLatch update = new CountDownLatch(updateCount);
    CountDownLatch remove = new CountDownLatch(removeCount);
    CountDownLatch clear = new CountDownLatch(clearCount);
    CountDownLatch unlinked = new CountDownLatch(1);

    MapDownlinkLifecycle<K, V> lifecycle = new MapDownlinkLifecycle<>();
    lifecycle
        .setOnLinked(() -> {
          if (linkState.get() == LinkState.Init && linked.getCount() == 1) {
            linkState.set(LinkState.Linked);
            linked.countDown();
          } else {
            fail(String.format("Illegal downlink state for a linked callback %s, latch count: %s", linkState.get(), linked.getCount()));
          }
        })
        .setOnUpdate((key, state, previous, value) -> {
          if (linkState.get() == LinkState.Synced && update.getCount() != 0) {
            update.countDown();
          } else {
            fail(String.format("Illegal downlink state for an update callback %s, latch count: %s", linkState.get(), update.getCount()));
          }
        })
        .setOnRemove((key, state, value) -> {
          if (linkState.get() == LinkState.Synced && remove.getCount() != 0) {
            remove.countDown();
          } else {
            fail(String.format("Illegal downlink state for a remove callback %s, latch count: %s", linkState.get(), remove.getCount()));
          }
        })
        .setOnClear((state) -> {
          if (linkState.get() == LinkState.Synced && clear.getCount() == 1) {
            clear.countDown();
            assertEquals(finalState, state);
          } else {
            fail(String.format("Illegal downlink state for a clear callback %s, latch count: %s", linkState.get(), clear.getCount()));
          }
        })
        .setOnSynced((value -> {
          if (linkState.get() == LinkState.Linked && synced.getCount() == 1) {
            linkState.set(LinkState.Synced);
            synced.countDown();
          } else {
            fail(String.format("Illegal downlink state for a synced callback %s, latch count: %s", linkState.get(), synced.getCount()));
          }
        }))
        .setOnUnlinked(() -> {
          if (linkState.get() == LinkState.Synced && unlinked.getCount() == 1) {
            linkState.set(LinkState.Unlinked);
            unlinked.countDown();
          } else {
            fail(String.format("Illegal downlink state for an unlinked callback %s, latch count: %s", linkState.get(), unlinked.getCount()));
          }
        });

    MapDownlinkState<K, V> state = new MapDownlinkState<>(Form.forClass(keyClass), Form.forClass(valueClass), lifecycle.getOnRemove());

    long ptr = lifecycleTest(
        lock,
        input.toString(),
        "host",
        "node",
        "lane",
        lifecycle.getOnLinked(),
        state.wrapOnSynced(lifecycle.getOnSynced()),
        state.wrapOnUpdate(lifecycle.getOnUpdate()),
        state.wrapOnRemove(lifecycle.getOnRemove()),
        state.wrapOnClear(lifecycle.getOnClear()),
        lifecycle.getOnUnlinked(),
        state.take(),
        state.drop()
    );

    awaitLatch(linked, 5, "linked");
    awaitLatch(synced, 5, "synced");
    awaitLatch(update, 5, "update");
    awaitLatch(remove, 5, "remove");
    awaitLatch(unlinked, 5, "unlinked");
    awaitLatch(clear, 5, "clear");
    awaitTrigger(lock, 10, "test lock");

    dropRuntime(ptr);
  }

  @Test
  void stateTest() throws SwimClientException, InterruptedException {
    runTestOk(
        String.class,
        Integer.class,
        2,
        new ConcurrentLinkedDeque<>(List.of(
            MapMessage.update("1", 1),
            MapMessage.update("2", 2),
            MapMessage.update("3", 3),
            MapMessage.update("4", 4),
            MapMessage.update("5", 5)
        )),
        new ConcurrentLinkedDeque<>(List.of(
            MapMessage.remove("5"),
            MapMessage.remove("4"),
            MapMessage.update("6", 6),
            MapMessage.update("7", 7),
            MapMessage.clear()
        )),
        Map.of(
            "1", 1,
            "2", 2,
            "3", 3,
            "6", 6,
            "7", 7
        )
    );
  }

  @Test
  void structuralStateTest() throws SwimClientException, InterruptedException {
    runTestOk(
        Integer.class,
        SuperClass.class,
        4,
        new ConcurrentLinkedDeque<>(List.of(
            MapMessage.update(1, new SubClassA(1, "a")),
            MapMessage.update(2, new SubClassA(2, "b")),
            MapMessage.update(3, new SubClassA(3, "c")),
            MapMessage.update(4, new SubClassA(4, "d")),
            MapMessage.update(5, new SubClassA(5, "e"))
        )),
        new ConcurrentLinkedDeque<>(List.of(
            MapMessage.remove(5),
            MapMessage.remove(4),
            MapMessage.update(6, new SubClassA(6, "f")),
            MapMessage.update(7, new SubClassA(7, "g")),
            MapMessage.take(4),
            MapMessage.drop(1),
            MapMessage.clear()
        )),
        Map.of(
            2, new SubClassA(2, "b"),
            3, new SubClassA(3, "c"),
            6, new SubClassA(6, "f")
        )
    );
  }

  @AutoForm(subTypes = {
      @AutoForm.Type(SubClassA.class),
      @AutoForm.Type(SubClassB.class),
  })
  @AutoForm.Tag("super")
  public static abstract class SuperClass {
    public int elem;

    public SuperClass() {

    }

    public SuperClass(int elem) {
      this.elem = elem;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SuperClass that = (SuperClass) o;
      return elem == that.elem;
    }

    @Override
    public int hashCode() {
      return Objects.hash(elem);
    }
  }

  @AutoForm
  @AutoForm.Tag("suba")
  public static class SubClassA extends SuperClass {
    public String fieldA;

    public SubClassA() {

    }

    public SubClassA(int elem, String fieldA) {
      super(elem);
      this.fieldA = fieldA;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      SubClassA subClassA = (SubClassA) o;
      return Objects.equals(fieldA, subClassA.fieldA);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), fieldA);
    }
  }

  @AutoForm
  @AutoForm.Tag("subb")
  public static class SubClassB extends SuperClass {
    public String fieldB;

    public SubClassB() {

    }

    public SubClassB(int elem, String fieldB) {
      super(elem);
      this.fieldB = fieldB;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      SubClassB subClassB = (SubClassB) o;
      return Objects.equals(fieldB, subClassB.fieldB);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), fieldB);
    }
  }

}
