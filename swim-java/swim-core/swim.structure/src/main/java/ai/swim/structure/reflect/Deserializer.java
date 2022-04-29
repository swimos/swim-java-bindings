package ai.swim.structure.reflect;

import ai.swim.recon.event.ReadEvent;

public class Deserializer {

  private static DeserializerFactory factory;

  interface Reader<P> {
  }

  interface Deserializer2<O> {
    O deserialize(Reader<ReadEvent> reader);
  }

  private static class DeserializerFactory {
    <O> Deserializer2<O> fromLayout(TypeLayout layout){
      return null;
    }
  }

  public static <O> O deserialize(Class<O> clazz, Reader<ReadEvent> reader) {
    TypeLayout typeLayout = TypeLayoutFactory.getInstance().forClass(clazz);
    Deserializer2<O> deserializer = factory.fromLayout(typeLayout);

    return deserializer.deserialize(reader);
  }

}
