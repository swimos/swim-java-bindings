package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadBlobValue;
import ai.swim.recon.event.ReadBooleanValue;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.recon.event.number.*;
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
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).value();
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
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).value();
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
        Long value = ((ReadLongValue) event).value();
        short shortValue = value.shortValue();

        if ((long) shortValue == shortValue) {
          return shortValue;
        } else {
          return null;
        }
      } else if (event.isReadInt()) {
        Integer value = ((ReadIntValue) event).value();
        short shortValue = value.shortValue();

        if ((int) shortValue == value) {
          return shortValue;
        } else {
          return null;
        }
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).value();
        try {
          return value.shortValueExact();
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).value();
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
      } else if (event.isReadBigDecimal()) {
        BigDecimal value = ((ReadBigDecimalValue) event).value();
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
        Long value = ((ReadLongValue) event).value();
        return value.floatValue();
      } else if (event.isReadBigInt()) {
        BigInteger value = ((ReadBigIntValue) event).value();

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
        BigDecimal value = ((ReadBigDecimalValue) event).value();

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

  public static final Recognizer<Character> CHARACTER = new SimpleRecognizer<>(true, "Character") {
    @Override
    public Character feed(ReadEvent event) {
      if (event.isText()) {
        String readTextValue = ((ReadTextValue) event).value();

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
        return readBigIntValue.value();
      } else if (event.isReadBigDecimal()) {
        ReadBigDecimalValue readBigDecimalValue = (ReadBigDecimalValue) event;
        try {
          return readBigDecimalValue.value().toBigIntegerExact();
        } catch (Exception ignored) {
          return null;
        }
      } else if (event.isReadLong()) {
        ReadLongValue readLongValue = (ReadLongValue) event;
        return BigInteger.valueOf(readLongValue.value());
      } else if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return BigInteger.valueOf((long) readIntValue.value());
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
        return new BigDecimal(readBigIntValue.value());
      } else if (event.isReadBigDecimal()) {
        ReadBigDecimalValue readBigDecimalValue = (ReadBigDecimalValue) event;
        return readBigDecimalValue.value();
      } else if (event.isReadLong()) {
        ReadLongValue readLongValue = (ReadLongValue) event;
        return BigDecimal.valueOf(readLongValue.value());
      } else if (event.isReadInt()) {
        ReadIntValue readIntValue = (ReadIntValue) event;
        return BigDecimal.valueOf((long) readIntValue.value());
      } else if (event.isReadFloat()) {
        ReadFloatValue readFloatValue = (ReadFloatValue) event;
        return BigDecimal.valueOf(readFloatValue.value());
      } else if (event.isReadDouble()) {
        ReadDoubleValue readDoubleValue = (ReadDoubleValue) event;
        return BigDecimal.valueOf(readDoubleValue.value());
      } else {
        return null;
      }
    }
  };

  public static final Recognizer<Number> NUMBER = new SimpleRecognizer<>(true, "Number") {
    @Override
    protected Number feed(ReadEvent event) {
      if (event.isReadInt()) {
        return ((ReadIntValue) event).value();
      } else if (event.isReadLong()) {
        return ((ReadLongValue) event).value();
      } else if (event.isReadFloat()) {
        return ((ReadFloatValue) event).value();
      } else if (event.isReadDouble()) {
        return ((ReadDoubleValue) event).value();
      } else if (event.isReadBigInt()) {
        return ((ReadBigIntValue) event).value();
      } else if (event.isReadBigDecimal()) {
        return ((ReadBigDecimalValue) event).value();
      } else {
        return null;
      }
    }
  };

}
