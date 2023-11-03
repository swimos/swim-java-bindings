package ai.swim.server.agent.task;

public class Schedule {
  private static final int INFINITE = -1;

  private int remaining;

  public Schedule(int remaining) {
    this.remaining = remaining;
  }

  void decrement() {
    if (remaining != INFINITE) {
      remaining -= 1;
    }
  }

  public int remainingRuns() {
    return remaining;
  }

  public boolean isScheduled() {
    return remaining > 0 || remaining == INFINITE;
  }
}
