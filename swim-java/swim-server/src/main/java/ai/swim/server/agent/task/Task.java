package ai.swim.server.agent.task;

import ai.swim.server.agent.AgentContext;
import java.util.ArrayList;
import java.util.UUID;

public class Task {
  private final AgentContext context;
  private final UUID id;
  private final Schedule schedule;
  private final Runnable runnable;

  public Task(AgentContext context, UUID id, Schedule schedule, Runnable runnable) {
    this.context = context;
    this.id = id;
    this.schedule = schedule;
    this.runnable = runnable;
  }

  public void cancel() {
    if (isScheduled()) {
      context.cancelTask(this);
    }
  }

  public UUID getId() {
    return id;
  }

  public boolean isScheduled() {
    return schedule.isScheduled();
  }

  void run() {
    if (!isScheduled()) {
      throw new IllegalStateException(String.format("Task %s is not scheduled", id));
    } else {
      schedule.decrement();
      runnable.run();
    }
  }
}
