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

package ai.swim.client.downlink.value;
import ai.swim.client.SwimClientException;
import ai.swim.client.downlink.DownlinkConfig;
import ai.swim.client.lifecycle.OnLinked;
import ai.swim.client.lifecycle.OnUnlinked;
import ai.swim.concurrent.Trigger;
import ai.swim.structure.Form;
import ai.swim.structure.Recon;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ValueDownlinkTest {

  static {
    System.loadLibrary("swim_client_test");
  }

  private static native long lifecycleTest(
      Trigger trigger,
      String input,
      String host,
      String node,
      String lane,
      Consumer<ByteBuffer> onEvent,
      OnLinked onLinked,
      Consumer<ByteBuffer> onSet,
      Consumer<ByteBuffer> onSynced,
      OnUnlinked onUnlinked
  );

  private static native void dropRuntime(long ptr);

  /**
   * Tests that the required JNI calls exist for opening a downlink.
   */
  @Test
  void simpleOpen() throws InterruptedException {
    Trigger trigger = new Trigger();
    TestValueDownlink<String> downlink = (TestValueDownlink<String>) new TestValueDownlinkBuilder<>(String.class, "host", "node", "lane", (build) -> {
      ValueDownlinkState<String> state = new ValueDownlinkState<>(Form.forClass(build.formType));
      Trigger stoppedBarrier = new Trigger();
      TestValueDownlink<String> testValueDownlink = new TestValueDownlink<>(stoppedBarrier, state);

      long ptr = lifecycleTest(
          trigger,
          "",
          build.host,
          build.node,
          build.lane,
          null,
          null,
          null,
          null,
          null
      );

      testValueDownlink.setClose(() -> dropRuntime(ptr));

      return testValueDownlink;
    }).open();

    downlink.close();
    awaitTrigger(trigger, 5, "downlink");
  }

  <I> void runTestOk(Class<I> clazz, ConcurrentLinkedDeque<I> syncEvents, ConcurrentLinkedDeque<I> events) throws InterruptedException, SwimClientException {
    StringBuilder input = new StringBuilder();
    input.append("@linked(node:node,lane:lane)\n");

    for (I event : syncEvents) {
      input.append(String.format("@event(node:node,lane:lane){%s}\n", Recon.toStringCompact(event)));
    }

    input.append("@synced(node:node,lane:lane)\n");

    for (I event : events) {
      input.append(String.format("@event(node:node,lane:lane){%s}\n", Recon.toStringCompact(event)));
    }

    input.append("@unlinked(node:node,lane:lane)\n");

    Trigger lock = new Trigger();
    AtomicReference<LinkState> linkState = new AtomicReference<>(LinkState.Init);
    AtomicReference<I> last = new AtomicReference<>(null);

    TestValueDownlinkBuilder<I> builder = new TestValueDownlinkBuilder<>(clazz, "host", "node", "lane", (build) -> {
      ValueDownlinkLifecycle<I> lifecycle = build.lifecycle;
      ValueDownlinkState<I> state = new ValueDownlinkState<>(Form.forClass(build.formType));
      Trigger stoppedBarrier = new Trigger();
      TestValueDownlink<I> testValueDownlink = new TestValueDownlink<>(stoppedBarrier, state);

      long ptr = lifecycleTest(
          lock,
          input.toString(),
          build.host,
          build.node,
          build.lane,
          state.wrapOnEvent(lifecycle.getOnEvent()),
          lifecycle.getOnLinked(),
          state.wrapOnSet(lifecycle.getOnSet()),
          state.wrapOnSynced(lifecycle.getOnSynced()),
          lifecycle.getOnUnlinked()
      );

      testValueDownlink.setClose(() -> dropRuntime(ptr));

      return testValueDownlink;
    });

    CountDownLatch linked = new CountDownLatch(1);
    CountDownLatch synced = new CountDownLatch(1);
    CountDownLatch event = new CountDownLatch(syncEvents.size() + events.size());
    CountDownLatch set = new CountDownLatch(syncEvents.size() + events.size());
    CountDownLatch unlinked = new CountDownLatch(1);

    TestValueDownlink<I> downlink = (TestValueDownlink<I>) builder
        .setOnLinked(() -> {
          if (linkState.get() == LinkState.Init && linked.getCount() == 1) {
            linkState.set(LinkState.Linked);
            linked.countDown();
          } else {
            fail(String.format("Illegal downlink state for a linked callback %s, latch count: %s", linkState.get(), linked.getCount()));
          }
        })
        .setOnEvent(val -> {
          switch (linkState.get()) {
            case Linked:
              if (event.getCount() == syncEvents.size() + events.size()) {
                event.countDown();
                assertEquals(syncEvents.peekFirst(), val);
                break;
              } else {
                fail(String.format("Illegal downlink state for an event callback %s, latch count: %s", linkState.get(), event.getCount()));
              }
            case Synced:
              if (event.getCount() == syncEvents.size() + events.size()) {
                event.countDown();
                assertEquals(events.peekFirst(), val);
                break;
              } else {
                fail(String.format("Event callback called more than once in the synced state. Count: %s", event.getCount()));
              }
          }
        })
        .setOnSet((oldValue, newValue) -> {
          switch (linkState.get()) {
            case Linked:
              if (set.getCount() == syncEvents.size() + events.size()) {
                set.countDown();
                assertEquals(last.get(), oldValue);
                assertEquals(syncEvents.pollFirst(), newValue);
                last.set(newValue);
                break;
              } else {
                fail(String.format("Illegal downlink state for a set callback %s, latch count: %s", linkState.get(), set.getCount()));
              }
            case Synced:
              if (set.getCount() == syncEvents.size() + events.size()) {
                set.countDown();
                assertEquals(last.get(), oldValue);
                assertEquals(events.pollFirst(), newValue);
                last.set(newValue);
                break;
              } else {
                fail(String.format("Set callback called more than once in the synced state. Count: %s", set.getCount()));
              }
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
        })
        .open();

    awaitLatch(linked, 5, "linked");
    awaitLatch(synced, 5, "synced");
    awaitLatch(event, 5, "event");
    awaitLatch(set, 5, "set");
    awaitLatch(unlinked, 5, "unlinked");
    awaitTrigger(lock, 10, "test lock");

    downlink.close();
  }

  @Test
  void testLifecyclesCalled() throws InterruptedException, SwimClientException {
    runTestOk(Integer.class, new ConcurrentLinkedDeque<>(List.of(13)), new ConcurrentLinkedDeque<>(List.of(14, 15)));
  }

  @Test
  void recordType() throws InterruptedException, SwimClientException {
    runTestOk(
        Envelope.class,
        new ConcurrentLinkedDeque<>(List.of(new Envelope("node0", "node1"))),
        new ConcurrentLinkedDeque<>(List.of(
            new Envelope("node1", "lane1"),
            new Envelope("node2", "lane2"),
            new LaneAddressed("node3", "lane3", 1),
            new LaneAddressed("node4", "lane4", 2)
        ))
    );
  }

  void awaitLatch(CountDownLatch latch, long time, String latchName) throws InterruptedException {
    if (!latch.await(time, TimeUnit.SECONDS)) {
      fail(String.format("%s latch elapsed before countdown reached", latchName));
    }
  }

  void awaitTrigger(Trigger trigger, long time, String latchName) throws InterruptedException {
    if (!trigger.awaitTrigger(time, TimeUnit.SECONDS)) {
      fail(String.format("%s latch elapsed before countdown reached", latchName));
    }
  }

  enum LinkState {
    Init,
    Linked,
    Synced,
    Unlinked
  }

  @AutoForm
  @AutoForm.Tag("event")
  public static class Event {
    public String node;
    public String lane;
    @AutoForm.Kind(FieldKind.Body)
    public BigInteger value;

    @Override
    public String toString() {
      return "Event{" +
          "node='" + node + '\'' +
          ", lane='" + lane + '\'' +
          ", value=" + value +
          '}';
    }
  }

  private static class TestValueDownlink<T> extends ValueDownlink<T> {
    private Runnable close;

    TestValueDownlink(Trigger trigger, ValueDownlinkState<T> state) {
      super(trigger, state);
    }

    public void close() {
      close.run();
    }

    public void setClose(Runnable close) {
      this.close = close;
    }
  }

  private static class TestValueDownlinkBuilder<T> extends ValueDownlinkBuilderModel<T> {
    private final Function<TestValueDownlinkBuilder<T>, TestValueDownlink<T>> builder;

    public TestValueDownlinkBuilder(Class<T> formType, String host, String node, String lane, Function<TestValueDownlinkBuilder<T>, TestValueDownlink<T>> builder) {
      super(formType, host, node, lane);
      this.builder = builder;
    }

    @Override
    public ValueDownlink<T> open() {
      return builder.apply(this);
    }
  }

  @AutoForm(subTypes = {
      @AutoForm.Type(LaneAddressed.class)
  })
  public static class Envelope {
    public String node;
    public String lane;

    public Envelope() {

    }

    public Envelope(String node, String lane) {
      this.node = node;
      this.lane = lane;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Envelope envelope = (Envelope) o;
      return Objects.equals(node, envelope.node) && Objects.equals(lane, envelope.lane);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, lane);
    }
  }

  @AutoForm
  public static class LaneAddressed extends Envelope {
    @AutoForm.Kind(FieldKind.Body)
    public int body;

    public LaneAddressed() {

    }

    public LaneAddressed(String node, String lane, int body) {
      super(node, lane);
      this.body = body;
    }
  }

  private static native <T> long driveDownlink(
      ValueDownlink<T> downlinkRef,
      Trigger stoppedBarrier,
      Trigger testBarrier,
      String host,
      String node,
      String lane,
      Consumer<ByteBuffer> onEvent,
      OnLinked onLinked,
      Consumer<ByteBuffer> onSet,
      Consumer<ByteBuffer> onSynced,
      OnUnlinked onUnlinked
  );

  private static native void dropSwimClient(long ptr);

  @Test
  void testWithServer() throws InterruptedException, SwimClientException {
    CountDownLatch linkedLatch = new CountDownLatch(1);
    CountDownLatch syncedLatch = new CountDownLatch(1);
    CountDownLatch eventLatch = new CountDownLatch(1);
    CountDownLatch setLatch = new CountDownLatch(1);
    CountDownLatch unlinkedLatch = new CountDownLatch(1);
    Trigger ffiBarrier = new Trigger();

    TestValueDownlinkBuilder<Integer> builder = new TestValueDownlinkBuilder<>(Integer.class, "ws://127.0.0.1", "node", "lane", (build) -> {
      ValueDownlinkLifecycle<Integer> lifecycle = build.lifecycle;
      ValueDownlinkState<Integer> state = new ValueDownlinkState<>(Form.forClass(build.formType));
      Trigger stoppedBarrier = new Trigger();
      TestValueDownlink<Integer> downlink = new TestValueDownlink<>(null, state);

      long ptr = driveDownlink(
          downlink,
          stoppedBarrier,
          ffiBarrier,
          build.host,
          build.node,
          build.lane,
          state.wrapOnEvent(lifecycle.getOnEvent()),
          lifecycle.getOnLinked(),
          state.wrapOnSet(lifecycle.getOnSet()),
          state.wrapOnSynced(lifecycle.getOnSynced()),
          lifecycle.getOnUnlinked()
      );

      downlink.setClose(() -> dropSwimClient(ptr));

      return downlink;
    });

    TestValueDownlink<Integer> downlink = (TestValueDownlink<Integer>) builder
        .setOnEvent(event -> {
          eventLatch.countDown();
          assertEquals(15, event);
        })
        .setOnLinked(linkedLatch::countDown)
        .setOnSet((oldValue, newValue) -> {
          setLatch.countDown();
          assertEquals(oldValue, 13);
          assertEquals(newValue, 15);
        }).setOnSynced(state -> {
          syncedLatch.countDown();
          assertEquals(state, 13);
        })
        .setOnUnlinked(unlinkedLatch::countDown)
        .open();

    awaitLatch(linkedLatch, 10, "linkedLatch");
    awaitLatch(syncedLatch, 10, "syncedLatch");
    awaitLatch(eventLatch, 10, "eventLatch");
    awaitLatch(setLatch, 10, "setLatch");
    awaitLatch(unlinkedLatch, 10, "unlinkedLatch");

    ffiBarrier.trigger();

    assertEquals(0, linkedLatch.getCount());
    assertEquals(0, syncedLatch.getCount());
    assertEquals(0, eventLatch.getCount());
    assertEquals(0, setLatch.getCount());
    assertEquals(0, unlinkedLatch.getCount());

    downlink.close();
  }

  @Test
  void invalidUrl() {
    Trigger barrier = new Trigger();
    assertThrows(SwimClientException.class, () -> {
      ValueDownlink<Object> downlink = new ValueDownlink<>(null, null) {
      };

      driveDownlink(
          downlink,
          new Trigger(),
          barrier,
          "swim.ai",
          "",
          "",
          null, null, null, null, null
      );
    }, "Failed to parse host URL: RelativeUrlWithoutBase");
  }

  @Test
  void testAwaitClosedOk() throws SwimClientException {
    Trigger ffiBarrier = new Trigger();

    TestValueDownlink<Integer> testDownlink = (TestValueDownlink<Integer>) new TestValueDownlinkBuilder<>(Integer.class, "ws://127.0.0.1", "node", "lane", (build) -> {
      ValueDownlinkState<Integer> state = new ValueDownlinkState<>(Form.forClass(build.formType));
      Trigger stoppedBarrier = new Trigger();
      TestValueDownlink<Integer> downlink = new TestValueDownlink<>(stoppedBarrier, state);

      long ptr = driveDownlink(
          downlink,
          stoppedBarrier,
          ffiBarrier,
          build.host,
          build.node,
          build.lane,
          null,
          null,
          null,
          null,
          null
      );

      downlink.setClose(() -> dropSwimClient(ptr));
      return downlink;
    }).open();

    try {
      testDownlink.awaitStopped();
    } catch (Throwable e) {
      testDownlink.close();
      throw new SwimClientException(e);
    }

    testDownlink.close();
  }

  private static native <T> long driveDownlinkError(
      ValueDownlink<T> downlinkRef,
      Trigger stoppedBarrier,
      Trigger testBarrier,
      Consumer<ByteBuffer> onEvent
  );

  @Test
  void testAwaitClosedError() throws SwimClientException {
    Trigger ffiBarrier = new Trigger();
    TestValueDownlink<Integer> testDownlink = (TestValueDownlink<Integer>) new TestValueDownlinkBuilder<>(Integer.class, "ws://127.0.0.1", "node", "lane", (build) -> {
      ValueDownlinkState<Integer> state = new ValueDownlinkState<>(Form.forClass(build.formType));
      Trigger stoppedBarrier = new Trigger();
      TestValueDownlink<Integer> downlink = new TestValueDownlink<>(stoppedBarrier, state);
      ValueDownlinkLifecycle<Integer> lifecycle = build.lifecycle;

      long ptr = driveDownlinkError(downlink, stoppedBarrier, ffiBarrier, state.wrapOnEvent(lifecycle.getOnEvent()));
      downlink.setClose(() -> dropSwimClient(ptr));
      return downlink;
    }).setOnEvent((i) -> {
    }).open();

    try {
      testDownlink.awaitStopped();
      fail("Expected awaitStopped to throw a SwimClientException");
    } catch (SwimClientException e) {
      assertEquals("Invalid frame body", e.getMessage());

      Throwable cause = e.getCause();

      assertNotNull(cause);
      assertTrue(cause instanceof RuntimeException);
      assertEquals("java.lang.RuntimeException: Found 'ReadTextValue{value='blah'}', expected: 'Integer' at: StringLocation{line=0, column=0, offset=4}", cause.getMessage());
    } finally {
      testDownlink.close();
    }
  }

  private static native void parsesConfig(byte[] downlinkConfig);

  @Test
  void parsesValidConfig() {
    byte[] downlinkConfig = new DownlinkConfig().toArray();
    parsesConfig(downlinkConfig);
  }

  @Test
  void throwsOnInvalidConfig() {
    assertThrows(SwimClientException.class, () -> {
      byte[] downlinkConfig = new byte[] {1, 2, 3, 4, 5};
      parsesConfig(downlinkConfig);
    }, "Failed to parse downlink configuration: \"Invalid buffer length\"");
  }

}
