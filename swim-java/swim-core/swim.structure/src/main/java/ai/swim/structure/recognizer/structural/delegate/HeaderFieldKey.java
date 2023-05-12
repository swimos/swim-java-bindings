package ai.swim.structure.recognizer.structural.delegate;

public abstract class HeaderFieldKey {
  public static final HeaderFieldKey HEADER_BODY = new HeaderFieldKey() {
    @Override
    public boolean isHeaderBody() {
      return true;
    }
  };

  public static HeaderFieldKey slot(String name) {
    return new HeaderSlotKey(name);
  }

  public boolean isHeaderBody() {
    return false;
  }

  public boolean isHeaderSlot() {
    return false;
  }

  public static class HeaderSlotKey extends HeaderFieldKey {

    private final String name;

    public HeaderSlotKey(String name) {
      this.name = name;
    }

    @Override
    public boolean isHeaderSlot() {
      return true;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "HeaderSlotKey{" +
        "name='" + name + '\'' +
        '}';
    }
  }
}
