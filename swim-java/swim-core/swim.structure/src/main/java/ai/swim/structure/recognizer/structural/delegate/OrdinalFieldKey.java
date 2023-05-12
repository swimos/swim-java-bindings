package ai.swim.structure.recognizer.structural.delegate;

public abstract class OrdinalFieldKey {

  public static final OrdinalFieldKey TAG = new OrdinalFieldKey() {
    @Override
    public boolean isTag() {
      return true;
    }
  };
  public static final OrdinalFieldKey HEADER = new OrdinalFieldKey() {
    @Override
    public boolean isHeader() {
      return true;
    }
  };
  public static final OrdinalFieldKey FIRST_ITEM = new OrdinalFieldKey() {
    @Override
    public boolean isFirstItem() {
      return true;
    }
  };

  public static OrdinalFieldKey attr(String name) {
    return new OrdinalFieldKeyAttr(name);
  }

  public boolean isHeader() {
    return false;
  }

  public boolean isFirstItem() {
    return false;
  }

  public boolean isAttr() {
    return false;
  }

  public boolean isTag() {
    return false;
  }

  public static class OrdinalFieldKeyAttr extends OrdinalFieldKey {
    private final String name;

    public OrdinalFieldKeyAttr(String name) {
      this.name = name;
    }

    @Override
    public boolean isAttr() {
      return true;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "OrdinalFieldKeyAttr{" +
        "name='" + name + '\'' +
        '}';
    }
  }
}
