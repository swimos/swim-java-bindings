package ai.swim.newstructure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.primitive.IntegerRecognizer;
import ai.swim.structure.recognizer.primitive.StringRecognizer;

class Prop {
  private String a;
  private Integer b;
  public Long c;

  public Prop() {

  }

  public void setA(String a) {
    this.a = a;
  }

  public void setB(Integer b) {
    this.b = b;
  }
}

abstract class Result<T> {

}

abstract class Proxy<T> {
  abstract Result<Proxy<T>> feed(ReadEvent readEvent);
}

interface ConstructorLambda<T> {
  T newInstance();
}

class FieldName {
  private String original;
  private String renamedTo;

  public FieldName(String original) {
    this.original = original;
  }

  public FieldName(String from, String to) {
    this.original = from;
    this.renamedTo = to;
  }

  public boolean matches(String name) {
    return this.original.equals(name) || name.equals(renamedTo);
  }

  public static FieldName of(String original) {
    return new FieldName(original);
  }

  public static FieldName renamed(String from, String to) {
    return new FieldName(from, to);
  }
}

abstract class FieldProxyBase<I, T> {
  private final String name;

  protected FieldProxyBase(String name) {
    this.name = name;
  }

  abstract FieldProxy<I, T> feed(ReadEvent readEvent);

  boolean isError() {
    return false;
  }

  String trap() {
    throw new IllegalStateException("FieldProxy is not in an error state");
  }
}

class FieldProxySet<I, T> extends FieldProxyBase<I, T> {
  public FieldProxySet(String name) {
    super(name);
  }

  @Override
  FieldProxy<I, T> feed(ReadEvent readEvent) {
    throw new IllegalStateException();
  }
}

class FieldProxy<I, T> extends FieldProxyBase<I, T> {
  private final Recognizer<T> recognizer;
  private ProxyFunction<I, T> proxy;

  public FieldProxy(String name, ProxyFunction<I, T> fn, Recognizer<T> recognizer) {
    super(name);
    this.proxy = fn;
    this.recognizer = recognizer;
  }

  public static <I, T> FieldProxy<I, T> of(String name, ProxyFunction<I, T> fn, Recognizer<T> recognizer) {
    return new FieldProxy<>(name, fn, recognizer);
  }

  @Override
  FieldProxy<I, T> feed(ReadEvent readEvent) {
    return null;
  }
}

@FunctionalInterface
interface ProxyFunction<I, T> {
  void apply(I instance, T target);
}

class PropProxy extends Proxy<Prop> {
  private static final ConstructorLambda<Prop> constructor = Prop::new;

  private final FieldProxy<Prop, String> fieldA = FieldProxy.of("a", Prop::setA, StringRecognizer.INSTANCE);
  private final FieldProxy<Prop, Integer> fieldB = FieldProxy.of("b", Prop::setB, IntegerRecognizer.PRIMITIVE);
  private final FieldProxy<Prop, Long> fieldC = FieldProxy.of("c", (prop, field) -> prop.c = field, null);
  private final Prop instance = new Prop();

  private FieldProxy<Prop, ?> current;

  public PropProxy() {

  }

  @Override
  Result<Proxy<Prop>> feed(ReadEvent readEvent) {
    return null;
  }
}

abstract class Schema<T> {

}

class SchemaProxy<T> extends Proxy<T> {
  private Proxy<T> inner;
  private Schema<T> schema;

  @Override
  Result<Proxy<T>> feed(ReadEvent readEvent) {
    return null;
  }
}
