package ai.swim.recon.models.events;

import ai.swim.recon.event.ReadEvent;

public abstract class EventOrEnd {
  private static EndEvent end;

  public static EventOrEnd end() {
    if (end == null) {
      end = new EndEvent();
    }

    return end;
  }

  public static EventOrEnd event(ReadEvent event, ParseEvents next) {
    return new Event(event, next);
  }

  public boolean isEvent() {
    return false;
  }

  public boolean isEnd() {
    return false;
  }

  public static class EndEvent extends EventOrEnd {
    @Override
    public boolean isEnd() {
      return true;
    }
  }
}
