package ai.swim.structure.recognizer;

import ai.swim.recon.event.*;

import java.util.function.Function;

@SuppressWarnings("unused")
public class ScalarRecognizer<T> extends Recognizer<T> {

  private static final Function<ReadEvent, Integer> INTEGER_FN = (event) -> {
    if (event.isNumber()) {
      ReadNumberValue readNumberValueEvent = (ReadNumberValue) event;
      return readNumberValueEvent.value().intValue();
    } else {
      return null;
    }
  };
  public static final Recognizer<Integer> BOXED_INTEGER = new ScalarRecognizer<>(INTEGER_FN, true, "Integer");
  public static final Recognizer<Integer> PRIMITIVE_INTEGER = new ScalarRecognizer<>(INTEGER_FN, false, "Integer");

  private static final Function<ReadEvent, Boolean> BOOLEAN_FN = (event) -> {
    if (event.isBoolean()) {
      ReadBooleanValue readBooleanValue = (ReadBooleanValue) event;
      return readBooleanValue.value();
    } else {
      return null;
    }
  };
  public static final Recognizer<Boolean> BOXED_BOOLEAN = new ScalarRecognizer<>(BOOLEAN_FN, true, "Boolean");
  public static final Recognizer<Boolean> PRIMITIVE_BOOLEAN = new ScalarRecognizer<>(BOOLEAN_FN, false, "Boolean");

  private static final Function<ReadEvent, Float> FLOAT_FN = (event) -> {
    if (event.isNumber()) {
      ReadNumberValue readNumberValueEvent = (ReadNumberValue) event;
      return readNumberValueEvent.value().floatValue();
    } else {
      return null;
    }
  };
  public static final Recognizer<Float> BOXED_FLOAT = new ScalarRecognizer<>(FLOAT_FN, true, "Float");
  public static final Recognizer<Float> PRIMITIVE_FLOAT = new ScalarRecognizer<>(FLOAT_FN, false, "Float");

  private static final Function<ReadEvent, byte[]> BLOB_FN = (event) -> {
    if (event.isBlob()) {
      ReadBlobValue readBlobValue = (ReadBlobValue) event;
      return readBlobValue.value();
    } else {
      return null;
    }
  };
  public static final Recognizer<Byte[]> BOXED_BLOB = new ScalarRecognizer<>(BLOB_FN, true, "Byte[]").map((blob) -> {
    // Is there not a nicer way to box this?
    Byte[] boxedBlob = new Byte[blob.length];

    for (int i = 0; i < blob.length; i++) {
      boxedBlob[i] = blob[i];
    }

    return boxedBlob;
  });
  public static final Recognizer<byte[]> PRIMITIVE_BLOB = new ScalarRecognizer<>(BLOB_FN, false, "byte[]");

  public static final Recognizer<String> STRING = new ScalarRecognizer<>((event) -> {
    if (event.isText()) {
      ReadTextValue readTextValue = (ReadTextValue) event;
      return readTextValue.value();
    } else {
      return null;
    }
  }, true, "String");

  private final Function<ReadEvent, T> recognizer;
  private final boolean allowExtant;
  private final String type;

  private ScalarRecognizer(Function<ReadEvent, T> recognizer, boolean allowExtant, String type) {
    this.recognizer = recognizer;
    this.allowExtant = allowExtant;
    this.type = type;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (allowExtant && event.isExtant()) {
      return Recognizer.done(null, this);
    } else {
      T value = recognizer.apply(event);
      if (value == null) {
        return Recognizer.error(new RuntimeException(String.format(String.format("Found '%s', expected: '%s'", event, type))));
      } else {
        return Recognizer.done(value, this);
      }
    }
  }

  @Override
  public Recognizer<T> reset() {
    return this;
  }
}
