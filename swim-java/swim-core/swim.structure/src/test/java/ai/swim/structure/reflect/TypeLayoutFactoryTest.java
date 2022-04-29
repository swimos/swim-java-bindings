package ai.swim.structure.reflect;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeLayoutFactoryTest {

  @Test
  void coreLookup() {
    TypeLayoutFactory typeFactory = TypeLayoutFactory.getInstance();
    assertEquals(typeFactory.forClass(Boolean.TYPE), CoreTypeLayout.of(Boolean.TYPE));
    assertEquals(typeFactory.forClass(Integer.TYPE), CoreTypeLayout.of(Integer.TYPE));
    assertEquals(typeFactory.forClass(Float.TYPE), CoreTypeLayout.of(Float.TYPE));
    assertEquals(typeFactory.forClass(Long.TYPE), CoreTypeLayout.of(Long.TYPE));
    assertEquals(typeFactory.forClass(Character.TYPE), CoreTypeLayout.of(Character.TYPE));
    assertEquals(typeFactory.forClass(Byte.TYPE), CoreTypeLayout.of(Byte.TYPE));
    assertEquals(typeFactory.forClass(Short.TYPE), CoreTypeLayout.of(Short.TYPE));
    assertEquals(typeFactory.forClass(String.class), CoreTypeLayout.of(String.class));
    assertEquals(typeFactory.forClass(Object.class), CoreTypeLayout.of(Object.class));
    assertEquals(typeFactory.forClass(Integer.class), CoreTypeLayout.of(Integer.class));
    assertEquals(typeFactory.forClass(Float.class), CoreTypeLayout.of(Float.class));
    assertEquals(typeFactory.forClass(Long.class), CoreTypeLayout.of(Long.class));
    assertEquals(typeFactory.forClass(Byte.class), CoreTypeLayout.of(Byte.class));
    assertEquals(typeFactory.forClass(Character.class), CoreTypeLayout.of(Character.class));
    assertEquals(typeFactory.forClass(Short.class), CoreTypeLayout.of(Short.class));
    assertEquals(typeFactory.forClass(Double.class), CoreTypeLayout.of(Double.class));
    assertEquals(typeFactory.forClass(Boolean.class), CoreTypeLayout.of(Boolean.class));
  }

  private static class Prop {
    Map<String, Integer> map;

    public Prop() {

    }
  }

  @Test
  void reflectClass() {
//    TypeLayoutFactory.getInstance().register(Prop.class, null);

//    TypeLayout typeLayout = TypeLayoutFactory.getInstance().forClass(Prop.class);
  }

  @Test
  void parameterizedType() {
    TypeLayout typeLayout = TypeLayoutFactory.getInstance().forType(Prop.class.getDeclaredFields()[0].getGenericType(), Generics.EMPTY);
    TypeLayout expected = new MapLayout(Map.class,
        new Generics("java.lang.String", CoreTypeLayout.of(String.class), "java.lang.Integer", CoreTypeLayout.of(Integer.class)),
        CoreTypeLayout.of(String.class),
        CoreTypeLayout.of(Integer.class),
        new TypeLayout[]{},
        null
    );

    assertEquals(typeLayout, expected);
  }

  private static class GenericProp<K extends List<Integer>, V extends List<String>> {
    private Map<K,V> map;

    public GenericProp() {

    }

    public void setMap(Map<K, V> map) {
      this.map = map;
    }
  }

  @Test
  void parameterizedType2() {
    TypeLayout typeLayout = TypeLayoutFactory.getInstance().forType(GenericProp.class.getDeclaredFields()[0].getGenericType(), Generics.EMPTY);
    System.out.println(typeLayout);
  }

}
