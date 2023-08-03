package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadBlobValue;
import ai.swim.recon.event.ReadBooleanValue;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.recon.event.number.ReadBigDecimalValue;
import ai.swim.recon.event.number.ReadBigIntValue;
import ai.swim.recon.event.number.ReadDoubleValue;
import ai.swim.recon.event.number.ReadFloatValue;
import ai.swim.recon.event.number.ReadIntValue;
import ai.swim.recon.event.number.ReadLongValue;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.SimpleRecognizer;
import java.math.BigDecimal;
import java.math.BigInteger;

@SuppressWarnings("unused")
public final class ScalarRecognizer<T> {

  public static final Recognizer<Byte> BYTE = new SimpleRecognizer<>(false, "Byte") {
    @Override
    public Byte feed(ReadEvent event) {
      if (event.isReadInt()) {
        int intValue = ((ReadIntValue) event).getValue();
        byte byteValue = Integer.valueOf(intValue).byteValue();
        if ((byte) intValue == byteValue) {
          return byteValue;
        } else {
          return null;
        }
      } else if (event.isReadLong()) {
        long longValue = ((ReadLongValue) event).getValue();
        byte byteValue = Long.valueOf(longValue).byteValue();
        if ((byte) longValue == byteValue) {
          return byteValue;
        } else {
          return null;
        }
      } else if (event.isReadBigInt()) {
        BigInteger bigInteger = ((ReadBigIntValue) event).getValue();
        try {
          return bigInteger.byteValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).getValue();
        try {
          return value.byteValueExact();
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
        return readIntValue.getValue();
      } else if (event.isReadLong()) {
        Long value = ((ReadLongValue) event).getValue();
        try {
          return Math.toIntExact(value);
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).getValue();
        try {
          return value.intValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).getValue();
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

  public static final Recognizer<Short> SHORT = new SimpleRecognizer<>(false, "Short") {
    @Override
    public Short feed(ReadEvent event) {
      if (event.isReadLong()) {
        Long value = ((ReadLongValue) event).getValue();
        short shortValue = value.shortValue();

        if ((long) shortValue == shortValue) {
          return shortValue;
        } else {
          return null;
        }
      } else if (event.isReadInt()) {
        Integer value = ((ReadIntValue) event).getValue();
        short shortValue = value.shortValue();

        if ((int) shortValue == value) {
          return shortValue;
        } else {
          return null;
        }
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).getValue();
        try {
          return value.shortValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).getValue();
        try {
          return value.shortValueExact();
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
        return readLongValue.getValue();
      } else if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return (long) readIntValue.getValue();
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).getValue();
        try {
          return value.longValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).getValue();
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
        return readBooleanValue.getValue();
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
        return floatValue.getValue();
      } else if (event.isReadDouble()) {
        Double doubleValue = ((ReadDoubleValue) event).getValue();
        float floatValue = doubleValue.floatValue();

        if ((double) floatValue == doubleValue) {
          return floatValue;
        } else {
          return null;
        }
      } else if (event.isReadInt()) {
        Integer value = ((ReadIntValue) event).getValue();
        return value.floatValue();
      } else if (event.isReadLong()) {
        Long value = ((ReadLongValue) event).getValue();
        return value.floatValue();
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).getValue();

        try {
          float floatValue = value.floatValue();
          if (floatValue == Float.MAX_VALUE || floatValue == Float.MIN_VALUE) {
            return null;
          } else {
            return floatValue;
          }
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).getValue();

        try {
          float floatValue = value.floatValue();
          if (floatValue == Float.MAX_VALUE || floatValue == Float.MIN_VALUE) {
            return null;
          } else {
            return floatValue;
          }
        } catch (Exception ignored) {
          return null;
        }
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Double> DOUBLE = new SimpleRecognizer<>(true, "Double") {
    @Override
    public Double feed(ReadEvent event) {
      if (event.isReadDouble()) {
        ReadDoubleValue doubleValue = (ReadDoubleValue) event;
        return doubleValue.getValue();
      } else if (event.isReadFloat()) {
        Float floatValue = ((ReadFloatValue) event).getValue();
        return floatValue.doubleValue();
      } else if (event.isReadInt()) {
        Integer value = ((ReadIntValue) event).getValue();
        return value.doubleValue();
      } else if (event.isReadLong()) {
        Long value = ((ReadLongValue) event).getValue();
        return value.doubleValue();
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).getValue();

        try {
          double doubleValue = value.doubleValue();
          if (doubleValue == Float.MAX_VALUE || doubleValue == Float.MIN_VALUE) {
            return null;
          } else {
            return doubleValue;
          }
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).getValue();

        try {
          double doubleValue = value.doubleValue();
          if (doubleValue == Float.MAX_VALUE || doubleValue == Float.MIN_VALUE) {
            return null;
          } else {
            return doubleValue;
          }
        } catch (Exception ignored) {
          return null;
        }
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
        return readBlobValue.getValue();
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
        return readTextValue.getValue();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Character> CHARACTER = new SimpleRecognizer<>(true, "Character") {
    @Override
    public Character feed(ReadEvent event) {
      if (event.isText()) {
        String readTextValue = ((ReadTextValue) event).getValue();

        if (readTextValue.length() == 1) {
          return readTextValue.charAt(0);
        } else {
          return null;
        }
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<BigInteger> BIG_INTEGER = new SimpleRecognizer<>(true, "BigInteger") {
    @Override
    public BigInteger feed(ReadEvent event) {
      if (event.isReadBigInt()) {
        ReadBigIntValue readBigIntValue = (ReadBigIntValue) event;
        return readBigIntValue.getValue();
      } else if (event.isReadBigDecimal()) {
        ReadBigDecimalValue readBigDecimalValue = (ReadBigDecimalValue) event;
        try {
          return readBigDecimalValue.getValue().toBigIntegerExact();
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadLong()) {
        ReadLongValue readLongValue = (ReadLongValue) event;
        return BigInteger.valueOf(readLongValue.getValue());
      } else if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return BigInteger.valueOf((long) readIntValue.getValue());
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<BigDecimal> BIG_DECIMAL = new SimpleRecognizer<>(true, "BigDecimal") {
    @Override
    public BigDecimal feed(ReadEvent event) {
      if (event.isReadBigInt()) {
        ReadBigIntValue readBigIntValue = (ReadBigIntValue) event;
        return new BigDecimal(readBigIntValue.getValue());
      } else if (event.isReadBigDecimal()) {
        ReadBigDecimalValue readBigDecimalValue = (ReadBigDecimalValue) event;
        return readBigDecimalValue.getValue();
      } else if (event.isReadLong()) {
        ReadLongValue readLongValue = (ReadLongValue) event;
        return BigDecimal.valueOf(readLongValue.getValue());
      } else if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return BigDecimal.valueOf((long) readIntValue.getValue());
      } else if (event.isReadFloat()) {
        ReadFloatValue readFloatValue = (ReadFloatValue) event;
        return BigDecimal.valueOf(readFloatValue.getValue());
      } else if (event.isReadDouble()) {
        ReadDoubleValue readDoubleValue = (ReadDoubleValue) event;
        return BigDecimal.valueOf(readDoubleValue.getValue());
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Number> NUMBER = new SimpleRecognizer<>(true, "Number") {
    @Override
    protected Number feed(ReadEvent event) {
      if (event.isReadInt()) {
        return ((ReadIntValue) event).getValue();
      } else if (event.isReadLong()) {
        return ((ReadLongValue) event).getValue();
      } else if (event.isReadFloat()) {
        return ((ReadFloatValue) event).getValue();
      } else if (event.isReadDouble()) {
        return ((ReadDoubleValue) event).getValue();
      } else if (event.isReadBigInt()) {
        return ((ReadBigIntValue) event).getValue();
      } else if (event.isReadBigDecimal()) {
        return ((ReadBigDecimalValue) event).getValue();
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Void> VOID = new Recognizer<>() {
    @Override
    public Recognizer<Void> feedEvent(ReadEvent event) {
      if (event.isExtant()) {
        return Recognizer.done(null, this);
      } else {
        return Recognizer.error(new RuntimeException(String.format(String.format(
            "Found '%s', expected: 'extant'",
            event))));
      }
    }

    @Override
    public Recognizer<Void> reset() {
      return this;
    }
  };

}
