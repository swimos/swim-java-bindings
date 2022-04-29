package ai.swim.structure.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.*;

class ReflectorTest {

  static final class Prop {
    private List<Map<String, Integer>> listy;

    public Prop(List<Map<String, Integer>> listy) {
      this.listy = listy;
    }

    public Prop() {

    }

    @Override
    public String toString() {
      return "Prop{" +
          "l=" + listy +
          '}';
    }

    public List<Map<String, Integer>> getListy() {
      return listy;
    }

    public void setListy(List<Map<String, Integer>> listy) {
      this.listy = listy;
    }
  }

  @Test
  void t() {
    Class<?> clazz = Prop.class;
    Constructor<?>[] constructors = clazz.getConstructors();
    for (Constructor<?> constructor : constructors) {
      Type[] types = constructor.getGenericParameterTypes();
      for (Type type : types) {
        System.out.println(type);
      }
    }
  }

}