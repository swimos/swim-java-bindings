package ai.swim.structure.recognizer.std;

import ai.swim.structure.recognizer.Recognizer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicRecognizer {

  public static final Recognizer<AtomicBoolean> ATOMIC_BOOLEAN = ScalarRecognizer.BOOLEAN.map(AtomicBoolean::new);

  public static final Recognizer<AtomicInteger> ATOMIC_INTEGER = ScalarRecognizer.INTEGER.map(AtomicInteger::new);

  public static final Recognizer<AtomicLong> ATOMIC_LONG = ScalarRecognizer.LONG.map(AtomicLong::new);

}
