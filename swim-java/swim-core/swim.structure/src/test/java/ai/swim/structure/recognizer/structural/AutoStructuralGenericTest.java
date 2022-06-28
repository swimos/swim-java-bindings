package ai.swim.structure.recognizer.structural;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.structure.FormParser;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.recognizer.proxy.TypeParameter;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class AutoStructuralGenericTest {

  @AutoForm("gen")
  public static class NestedGenerics<G extends Number, A extends Number, T extends CharSequence> {
    public G generic;
    public Typed<A, T> a;
    public boolean c;

    public NestedGenerics() {

    }

    public NestedGenerics(G generic, Typed<A, T> a, boolean c) {
      this.generic = generic;
      this.a = a;
      this.c = c;
    }


    @Override
    public String toString() {
      return "SimpleGeneric{" +
          "generic=" + generic +
          ", a=" + a +
          ", c=" + c +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof NestedGenerics)) return false;
      NestedGenerics<?, ?, ?> that = (NestedGenerics<?, ?, ?>) o;
      return c == that.c && Objects.equals(generic, that.generic) && Objects.equals(a, that.a);
    }

    @Override
    public int hashCode() {
      return Objects.hash(generic, a, c);
    }
  }

  @AutoForm
  public static class Typed<A extends Number, T extends CharSequence> {
    public A a;
    public T t;

    public Typed() {

    }

    public Typed(A a, T t) {
      this.a = a;
      this.t = t;
    }

    @Override
    public String toString() {
      return "Typed{" +
          "a=" + a +
          ", t=" + t +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Typed)) return false;
      Typed<?, ?> typed = (Typed<?, ?>) o;
      return Objects.equals(a, typed.a) && Objects.equals(t, typed.t);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, t);
    }
  }

  @Test
  void testInferred() {
    Recognizer<NestedGenerics<Integer, Integer, String>> recognizer = new NestedGenericsRecognizer<>();
    Parser<NestedGenerics<Integer, Integer, String>> parser = new FormParser<>(recognizer);

    parser = parser.feed(Input.string("@gen{generic:1,a:@Typed{a:2,t:text},c:true}"));

    assertTrue(parser.isDone());
    assertEquals(parser.bind(), new NestedGenerics<>(1, new Typed<>(2, "text"), true));
  }

  <G extends Number, A extends Number, T extends CharSequence> void runNestedTestOk(TypeParameter<G> gTy, TypeParameter<A> aTy, TypeParameter<T> tTy, String input, NestedGenerics<G, A, T> expected) {
    Recognizer<NestedGenerics<G, A, T>> recognizer = new NestedGenericsRecognizer<>(gTy, aTy, tTy);
    Parser<NestedGenerics<G, A, T>> parser = new FormParser<>(recognizer);

    parser = parser.feed(Input.string(input));
    assertEquals(parser.bind(), expected);
  }

  @Test
  void testFromTypeParameters() {
    runNestedTestOk(
        TypeParameter.from(Integer.class),
        TypeParameter.from(Integer.class),
        TypeParameter.from(String.class),
        "@gen{generic:1,a:@Typed{a:2,t:text},c:true}",
        new NestedGenerics<>(1, new Typed<>(2, "text"), true)
    );

    runNestedTestOk(
        TypeParameter.from(Integer.class),
        TypeParameter.from(Long.class),
        null,
        "@gen{generic:1,a:@Typed{a:2,t:text},c:false}",
        new NestedGenerics<>(1, new Typed<>(2L, "text"), false)
    );

    runNestedTestOk(
        null,
        null,
        null,
        "@gen{generic:1,a:@Typed{a:2,t:text},c:true}",
        new NestedGenerics<>(1, new Typed<>(2, "text"), true)
    );

    runNestedTestOk(
        null,
        TypeParameter.from(() -> ScalarRecognizer.INTEGER),
        null,
        "@gen{generic:1,a:@Typed{a:2,t:text},c:true}",
        new NestedGenerics<>(1, new Typed<>(2, "text"), true)
    );
  }

  <G extends Number, A extends Number, T extends CharSequence> void runNestedTestErr(TypeParameter<G> gTy, TypeParameter<A> aTy, TypeParameter<T> tTy, String input) {
    Recognizer<NestedGenerics<G, A, T>> recognizer = new NestedGenericsRecognizer<>(gTy, aTy, tTy);
    Parser<NestedGenerics<G, A, T>> parser = new FormParser<>(recognizer);

    parser = parser.feed(Input.string(input));
    assertTrue(parser.isError());
  }

  @AutoForm
  public static class Clazz<C, A, T> {
    public C c;
    public A a;
    public I<T> i;

    public Clazz() {
    }

    public Clazz(C c, A a, I<T> i) {
      this.c = c;
      this.a = a;
      this.i = i;
    }

    @Override
    public String toString() {
      return "Clazz{" +
          "c=" + c +
          ", a=" + a +
          ", i=" + i +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Clazz)) return false;
      Clazz<?, ?, ?> clazz = (Clazz<?, ?, ?>) o;
      return Objects.equals(c, clazz.c) && Objects.equals(a, clazz.a) && Objects.equals(i, clazz.i);
    }

    @Override
    public int hashCode() {
      return Objects.hash(c, a, i);
    }
  }

  @AutoForm
  public static class I<T> {
    public T t;

    public I() {

    }

    public I(T t) {
      this.t = t;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof I)) return false;
      I<?> i = (I<?>) o;
      return Objects.equals(t, i.t);
    }

    @Override
    public int hashCode() {
      return Objects.hash(t);
    }

    @Override
    public String toString() {
      return "I{" +
          "t=" + t +
          '}';
    }
  }

  @Test
  void testFromTypeParametersErr() {
    runNestedTestErr(
        TypeParameter.from(Integer.class),
        TypeParameter.from(Integer.class),
        TypeParameter.from(String.class),
        "@gen{generic:1,a:@Typed{a:2,t:1},c:true}"
    );
    runNestedTestErr(
        null,
        null,
        null,
        "@gen{generic:1,a:@Typed{a:\"\",t:1},c:true}"
    );

    assertThrows(RecognizerException.class, () -> new ClazzRecognizer<>(TypeParameter.from(Void.class), TypeParameter.from(Integer.class), TypeParameter.from(Long.class)));
    assertThrows(RecognizerException.class, () -> new ClazzRecognizer<>(TypeParameter.from(Integer.class), TypeParameter.from(Long.class), TypeParameter.from(Void.class)));
  }

  @Test
  void untyped() {
    Recognizer<Clazz<Integer, Long, Object>> recognizer = new ClazzRecognizer<>(
        TypeParameter.from(Integer.class),
        TypeParameter.from(Long.class),
        TypeParameter.untyped()
    );

    Parser<Clazz<Integer, Long, Object>> parser = new FormParser<>(recognizer);

    parser = parser.feed(Input.string("@Clazz{c:1,a:2,i:@I{t:{{4},{5},{6}}}}"));

    assertTrue(parser.isDone());
    assertEquals(new Clazz<>(1, 2L, new I<>(List.of(List.of(4), List.of(5), List.of(6)))), parser.bind());
  }

  @AutoForm
  public static class Col<C extends Collection<T>, T extends Number> {
    public C collection;
    public List<C> list;
    public T id;

    public Col() {

    }

    public Col(C collection, List<C> list, T id) {
      this.collection = collection;
      this.list = list;
      this.id = id;
    }

    @Override
    public String toString() {
      return "Col{" +
          "collection=" + collection +
          ", list=" + list +
          ", id=" + id +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Col)) return false;
      Col<?, ?> col = (Col<?, ?>) o;
      return Objects.equals(collection, col.collection) && Objects.equals(list, col.list) && Objects.equals(id, col.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(collection, list, id);
    }
  }


}
