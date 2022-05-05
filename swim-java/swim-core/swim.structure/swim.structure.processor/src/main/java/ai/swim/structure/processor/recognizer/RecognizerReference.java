package ai.swim.structure.processor.recognizer;

public class RecognizerReference extends RecognizerModel {

  private final String initializer;

  public RecognizerReference(String initializer) {
    this.initializer = initializer;
  }

  @Override
  public String initializer() {
    return this.initializer;
  }

  public static class Formatter {
    private final String packageName;

    public Formatter(String packageName) {
      this.packageName = packageName;
    }

    public RecognizerReference recognizerFor(String name) {
      return new RecognizerReference(String.format("%s.%s", this.packageName, name));
    }
  }
}

