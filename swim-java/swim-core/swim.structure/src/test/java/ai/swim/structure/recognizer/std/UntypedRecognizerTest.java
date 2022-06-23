package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class UntypedRecognizerTest {

  <T> void runTestOk(Recognizer<T> recognizer, T expected) {
    assertTrue(recognizer.isDone());
    assertEquals(recognizer.bind(), expected);
  }

  <T> void runTestErr(Recognizer<T> recognizer) {
    assertTrue(recognizer.isError());
  }

  @Test
  void testShort() {
    runTestOk(ScalarRecognizer.SHORT.feedEvent(ReadEvent.number((short) 1)), (short) 1);
    runTestOk(ScalarRecognizer.SHORT.feedEvent(ReadEvent.number(1)), (short) 1);
    runTestOk(ScalarRecognizer.SHORT.feedEvent(ReadEvent.number(Long.valueOf(123))), (short) 123);
    runTestOk(ScalarRecognizer.SHORT.feedEvent(ReadEvent.number(BigInteger.TEN)), (short) 10);
    runTestOk(ScalarRecognizer.SHORT.feedEvent(ReadEvent.number(BigDecimal.TEN)), (short) 10);
    runTestErr(ScalarRecognizer.SHORT.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testByte() {
    runTestOk(ScalarRecognizer.BYTE.feedEvent(ReadEvent.number((byte) 1)), (byte) 1);
    runTestOk(ScalarRecognizer.BYTE.feedEvent(ReadEvent.number(1)), (byte) 1);
    runTestOk(ScalarRecognizer.BYTE.feedEvent(ReadEvent.number(Long.valueOf(123))), (byte) 123);
    runTestOk(ScalarRecognizer.BYTE.feedEvent(ReadEvent.number(BigInteger.TEN)), (byte) 10);
    runTestOk(ScalarRecognizer.BYTE.feedEvent(ReadEvent.number(BigDecimal.TEN)), (byte) 10);
    runTestErr(ScalarRecognizer.BYTE.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testBool() {
    runTestOk(ScalarRecognizer.BOOLEAN.feedEvent(ReadEvent.bool(true)), true);
    runTestOk(ScalarRecognizer.BOOLEAN.feedEvent(ReadEvent.bool(false)), false);
    runTestErr(ScalarRecognizer.BOOLEAN.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testInt() {
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(1)), 1);
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(Integer.MAX_VALUE)), Integer.MAX_VALUE);
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(Long.valueOf(123))), 123);
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(BigInteger.TEN)), 10);
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(BigDecimal.TEN)), 10);
    runTestErr(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testLong() {
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(1L)), 1L);
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(Long.MAX_VALUE)), Long.MAX_VALUE);
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(123)), 123L);
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(BigInteger.TEN)), 10L);
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(BigDecimal.TEN)), 10L);
    runTestErr(ScalarRecognizer.LONG.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testFloat() {
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(1L)), 1f);
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(1)), 1f);
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(100d)), 100f);
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(BigInteger.TEN)), 10f);
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(BigDecimal.TEN)), 10f);
    runTestErr(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testBlob() {
    Recognizer<byte[]> recognizer = ScalarRecognizer.BLOB.feedEvent(ReadEvent.blob(new byte[]{1, 2, 3}));
    assertTrue(recognizer.isDone());
    assertArrayEquals(recognizer.bind(), new byte[]{1, 2, 3});

    runTestErr(ScalarRecognizer.BLOB.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testString() {
    runTestOk(ScalarRecognizer.STRING.feedEvent(ReadEvent.text("blob")), "blob");
    runTestErr(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testCharacter() {
    runTestOk(ScalarRecognizer.CHAR.feedEvent(ReadEvent.text("b")), 'b');
    runTestErr(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testBigInteger() {
    runTestOk(ScalarRecognizer.BIG_INTEGER.feedEvent(ReadEvent.number(1L)), BigInteger.valueOf(1L));
    runTestOk(ScalarRecognizer.BIG_INTEGER.feedEvent(ReadEvent.number(Long.MAX_VALUE)), BigInteger.valueOf(Long.MAX_VALUE));
    runTestOk(ScalarRecognizer.BIG_INTEGER.feedEvent(ReadEvent.number(123)), BigInteger.valueOf(123L));
    runTestOk(ScalarRecognizer.BIG_INTEGER.feedEvent(ReadEvent.number(BigInteger.TEN)), BigInteger.TEN);
    runTestOk(ScalarRecognizer.BIG_INTEGER.feedEvent(ReadEvent.number(BigDecimal.TEN)), BigInteger.TEN);
    runTestErr(ScalarRecognizer.BIG_INTEGER.feedEvent(ReadEvent.endAttribute()));
  }

  @Test
  void testBigDecimal() {
    runTestOk(ScalarRecognizer.BIG_DECIMAL.feedEvent(ReadEvent.number(1L)), BigDecimal.valueOf(1L));
    runTestOk(ScalarRecognizer.BIG_DECIMAL.feedEvent(ReadEvent.number(1)), BigDecimal.valueOf(1L));
    runTestOk(ScalarRecognizer.BIG_DECIMAL.feedEvent(ReadEvent.number(100d)), BigDecimal.valueOf(100.0));
    runTestOk(ScalarRecognizer.BIG_DECIMAL.feedEvent(ReadEvent.number(BigInteger.TEN)), BigDecimal.TEN);
    runTestOk(ScalarRecognizer.BIG_DECIMAL.feedEvent(ReadEvent.number(BigDecimal.TEN)), BigDecimal.TEN);
    runTestErr(ScalarRecognizer.BIG_DECIMAL.feedEvent(ReadEvent.endAttribute()));
  }

}