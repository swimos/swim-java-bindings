package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Prop {

  private abstract static class Reader<Y> {
  }

  interface Deserializer<T> {
    T deserialize(Reader<ReadEvent> reader);
  }

  @FunctionalInterface
  interface DeserializerFactory<T> {
    Deserializer<T> build();
  }

  private static class PropDeserializer implements Deserializer<Integer> {
    @Override
    public Integer deserialize(Reader<ReadEvent> reader) {
      return 1;
    }
  }

  @Test
  void t() {
    DeserializerFactory<Integer> fac = PropDeserializer::new;
    Deserializer<Integer> deserializer = fac.build();
    assertEquals(deserializer.deserialize(null), 1);
  }

}
