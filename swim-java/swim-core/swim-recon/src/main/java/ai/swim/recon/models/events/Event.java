package ai.swim.recon.models.events;

import ai.swim.recon.event.ReadEvent;

public class Event {
  private final ReadEvent event;
  private final ParseEvents next;

  public Event(ReadEvent event, ParseEvents next) {
    this.event = event;
    this.next = next;
  }

  public ReadEvent getEvent() {
    return event;
  }

  public ParseEvents getNext() {
    return next;
  }

}
