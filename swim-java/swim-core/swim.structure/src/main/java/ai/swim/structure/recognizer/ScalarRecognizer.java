package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadBlobValue;
import ai.swim.recon.event.ReadBooleanValue;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.recon.event.number.ReadFloatValue;
import ai.swim.recon.event.number.ReadIntValue;
import ai.swim.recon.event.number.ReadLongValue;

@SuppressWarnings("unused")
public abstract class ScalarRecognizer<T> extends Recognizer<T> {

  public static final Recognizer<Integer> INTEGER = new ScalarRecognizer<>(false, "Integer") {
    @Override
    Integer feed(ReadEvent event) {
      if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return readIntValue.value();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Long> LONG = new ScalarRecognizer<>(false, "Integer") {
    @Override
    Long feed(ReadEvent event) {
      if (event.isReadLong()) {
        ReadLongValue readLongValue = (ReadLongValue) event;
        return readLongValue.value();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Boolean> BOOLEAN = new ScalarRecognizer<>(false, "Boolean") {
    @Override
    Boolean feed(ReadEvent event) {
      if (event.isBoolean()) {
        ReadBooleanValue readBooleanValue = (ReadBooleanValue) event;
        return readBooleanValue.value();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Float> FLOAT = new ScalarRecognizer<>(true, "Float") {
    @Override
    public Float feed(ReadEvent event) {
      if (event.isReadFloat()) {
        ReadFloatValue floatValue = (ReadFloatValue) event;
        return floatValue.value();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<byte[]> BLOB = new ScalarRecognizer<>(false, "byte[]") {
    @Override
    byte[] feed(ReadEvent event) {
      if (event.isBlob()) {
        ReadBlobValue readBlobValue = (ReadBlobValue) event;
        return readBlobValue.value();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<String> STRING = new ScalarRecognizer<>(true, "String") {
    @Override
    String feed(ReadEvent event) {
      if (event.isText()) {
        ReadTextValue readTextValue = (ReadTextValue) event;
        return readTextValue.value();
      } else {
        return null;
      }
    }
  };

  private final boolean allowExtant;
  private final String type;

  private ScalarRecognizer(boolean allowExtant, String type) {
    this.allowExtant = allowExtant;
    this.type = type;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (allowExtant && event.isExtant()) {
      return Recognizer.done(null, this);
    } else {
      T value = feed(event);
      if (value == null) {
        return Recognizer.error(new RuntimeException(String.format(String.format("Found '%s', expected: '%s'", event, type))));
      } else {
        return Recognizer.done(value, this);
      }
    }
  }

  abstract T feed(ReadEvent event);

  @Override
  public Recognizer<T> reset() {
    return this;
  }
}
