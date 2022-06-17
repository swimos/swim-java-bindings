package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadBlobValue;
import ai.swim.recon.event.ReadBooleanValue;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.recon.event.number.*;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.SimpleRecognizer;

import java.math.BigInteger;

@SuppressWarnings("unused")
public final class ScalarRecognizer<T> {

  public static final Recognizer<Byte> BYTE = new SimpleRecognizer<>(false, "Byte") {
    @Override
    public Byte feed(ReadEvent event) {
      if (event.isReadInt()) {
        int intValue = ((ReadIntValue) event).value();
        byte byteValue = Integer.valueOf(intValue).byteValue();
        if ((byte) intValue == byteValue) {
          return byteValue;
        } else {
          return null;
        }
      } else if (event.isReadLong()) {
        long longValue = ((ReadLongValue) event).value();
        byte byteValue = Long.valueOf(longValue).byteValue();
        if ((byte) longValue == byteValue) {
          return byteValue;
        } else {
          return null;
        }
      } else if (event.isReadBigInt()) {
        BigInteger bigInteger = ((ReadBigIntValue) event).value();
        try {
          return bigInteger.byteValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Integer> INTEGER = new SimpleRecognizer<>(false, "Integer") {
    @Override
    public Integer feed(ReadEvent event) {
      if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return readIntValue.value();
      } else if (event.isReadLong()) {
        Long value = ((ReadLongValue) event).value();
        try {
          return Math.toIntExact(value);
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).value();
        try {
          return value.intValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Long> LONG = new SimpleRecognizer<>(false, "Long") {
    @Override
    public Long feed(ReadEvent event) {
      if (event.isReadLong()) {
        ReadLongValue readLongValue = (ReadLongValue) event;
        return readLongValue.value();
      } else if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return (long) readIntValue.value();
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).value();
        try {
          return value.longValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Boolean> BOOLEAN = new SimpleRecognizer<>(false, "Boolean") {
    @Override
    public Boolean feed(ReadEvent event) {
      if (event.isBoolean()) {
        ReadBooleanValue readBooleanValue = (ReadBooleanValue) event;
        return readBooleanValue.value();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Float> FLOAT = new SimpleRecognizer<>(true, "Float") {
    @Override
    public Float feed(ReadEvent event) {
      if (event.isReadFloat()) {
        ReadFloatValue floatValue = (ReadFloatValue) event;
        return floatValue.value();
      } else if (event.isReadDouble()) {
        Double doubleValue = ((ReadDoubleValue) event).value();
        float floatValue = doubleValue.floatValue();

        if ((double) floatValue == doubleValue) {
          return floatValue;
        } else {
          return null;
        }
      } else if (event.isReadInt()) {
        Integer value = ((ReadIntValue) event).value();
        return value.floatValue();
      } else if (event.isReadLong()) {
        //todo

      } else {
        return null;
      }
    }
  };

  public static final Recognizer<byte[]> BLOB = new SimpleRecognizer<>(false, "byte[]") {
    @Override
    public byte[] feed(ReadEvent event) {
      if (event.isBlob()) {
        ReadBlobValue readBlobValue = (ReadBlobValue) event;
        return readBlobValue.value();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<String> STRING = new SimpleRecognizer<>(true, "String") {
    @Override
    public String feed(ReadEvent event) {
      if (event.isText()) {
        ReadTextValue readTextValue = (ReadTextValue) event;
        return readTextValue.value();
      } else {
        return null;
      }
    }
  };

}
