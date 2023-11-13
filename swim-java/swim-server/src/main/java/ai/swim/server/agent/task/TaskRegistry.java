package ai.swim.server.agent.task;

import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.call.CallContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TaskRegistry {
  private final Map<UUID, Task> tasks;

  public TaskRegistry() {
    this.tasks = new HashMap<>();
  }

  public void runTask(UUID id, boolean remove) {
    Task task = tasks.get(id);

    if (task == null) {
      throw new NullPointerException("Missing task: " + id);
    } else {
      try {
        CallContext.enter();
        if (task.isScheduled()) {
          task.run();
        } else {
          tasks.remove(id);
        }
      } catch (Throwable e) {
        tasks.remove(id);
        throw e;
      } finally {
        CallContext.exit();
      }

      if (remove) {
        tasks.remove(id);
      }
    }
  }

  public Task registerTask(AgentContext context, Schedule schedule, Runnable runnable) {
    while (true) {
      UUID id = UUID.randomUUID();
      if (!tasks.containsKey(id)) {
        Task task = new Task(context, id, schedule, runnable);
        tasks.put(id, task);
        return task;
      }
    }
  }

  public void cancelTask(Task task) {
    tasks.remove(task.getId());
  }
}
