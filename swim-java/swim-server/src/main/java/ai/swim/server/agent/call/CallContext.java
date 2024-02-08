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

/**
 * Method invocation context tracking to ensure that the Java runtime does not call into the Rust runtime outside
 * a call that was originally made from Rust. This is a safety guard until adhoc calls from Java -> Rust have been
 * implemented.
 */
public class CallContext {
  private static final ThreadLocal<Object> CALL_CONTEXT = new ThreadLocal<>();

  public static void enter() {
    CALL_CONTEXT.set(new Object());
  }

  public static void exit() {
    CALL_CONTEXT.remove();
  }

  public static void check() {
    boolean entered = CALL_CONTEXT.get() != null;
    if (!entered) {
      throw CallContextException.illegalAccess(ThreadInfo.build());
    }
  }

  public static class ThreadInfo {
    private final String name;
    private final ThreadGroup threadGroup;
    private final long id;

    public ThreadInfo(String name, ThreadGroup threadGroup, long id) {
      this.name = name;
      this.threadGroup = threadGroup;
      this.id = id;
    }

    public static ThreadInfo build() {
      Thread thread = Thread.currentThread();
      return new ThreadInfo(thread.getName(), thread.getThreadGroup(), thread.getId());
    }

    @Override
    public String toString() {
      return "ThreadInfo{" +
          "name='" + name + '\'' +
          ", threadGroup=" + threadGroup +
          ", id=" + id +
          '}';
    }
  }

}
