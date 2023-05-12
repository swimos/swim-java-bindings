package ai.swim.structure.recognizer.structural.labelled;

public abstract class LabelledFieldKey {
  public static LabelledFieldKey TAG = new LabelledFieldKey() {
    @Override
    public boolean isTag() {
      return true;
    }
  };
  public static LabelledFieldKey HEADER = new LabelledFieldKey() {
    @Override
    public boolean isHeader() {
      return true;
    }
  };

  public static LabelledFieldKey attr(String name) {
    return new AttrFieldKey(name);
  }

  public static LabelledFieldKey item(String name) {
    return new ItemFieldKey(name);
  }

  public boolean isTag() {
    return false;
  }

  public boolean isHeader() {
    return false;
  }

  public boolean isAttribute() {
    return false;
  }

  public boolean isItem() {
    return false;
  }

  public static class AttrFieldKey extends LabelledFieldKey {
    private final String key;

    public AttrFieldKey(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    @Override
    public boolean isAttribute() {
      return true;
    }

    @Override
    public String toString() {
      return "AttrFieldKey{" +
        "key='" + key + '\'' +
        '}';
    }
  }

  public static class ItemFieldKey extends LabelledFieldKey {

    private final String name;

    public ItemFieldKey(String name) {
      this.name = name;
    }

    @Override
    public boolean isItem() {
      return true;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "ItemFieldKey{" +
        "name='" + name + '\'' +
        '}';
    }
  }
}
