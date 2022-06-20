package ai.swim.structure.recognizer.structural.delegate;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.structure.FormParser;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AutoDelegateTest {

  <T> void runTestOk(Recognizer<T> recognizer, T expected, String input) {
    Parser<T> parser = new FormParser<>(recognizer);
    parser = parser.feed(Input.string(input));
    if (parser.isDone()) {
      assertEquals(parser.bind(), expected);
    } else if (parser.isError()) {
      fail(((ParserError<T>) parser).cause());
    } else {
      fail();
    }
  }

  @AutoForm
  public static class PropClass {
    @AutoForm.Kind(FieldKind.Header)
    private int a;
    @AutoForm.Kind(FieldKind.Header)
    private String b;
    @AutoForm.Kind(FieldKind.Body)
    private String c;

    public PropClass() {

    }

    public PropClass(int a, String b, String c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }

    public void setA(int a) {
      this.a = a;
    }

    public void setB(String b) {
      this.b = b;
    }

    public void setC(String c) {
      this.c = c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PropClass)) return false;
      PropClass propClass = (PropClass) o;
      return a == propClass.a && Objects.equals(b, propClass.b) && Objects.equals(c, propClass.c);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, c);
    }

    @Override
    public String toString() {
      return "PropClass{" +
          "a=" + a +
          ", b='" + b + '\'' +
          ", c='" + c + '\'' +
          '}';
    }
  }

  @Test
  void testFieldManipulation() {
    runTestOk(new PropClassRecognizer(), new PropClass(1, "b", "c"), "@PropClass(a:1,b:b){c}");
  }

  @AutoForm
  public static class PropClass2 {
    @AutoForm.Kind(FieldKind.Attr)
    private int a;
    @AutoForm.Kind(FieldKind.Header)
    private int b;
    @AutoForm.Kind(FieldKind.Body)
    private int c;

    public PropClass2() {

    }

    public PropClass2(int a, int b, int c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }

    public void setA(int a) {
      this.a = a;
    }

    public void setB(int b) {
      this.b = b;
    }

    public void setC(int c) {
      this.c = c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PropClass2)) return false;
      PropClass2 that = (PropClass2) o;
      return a == that.a && Objects.equals(b, that.b) && Objects.equals(c, that.c);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, c);
    }

    @Override
    public String toString() {
      return "PropClass2{" +
          "a=" + a +
          ", b='" + b + '\'' +
          ", c='" + c + '\'' +
          '}';
    }
  }

  @Test
  void testFieldManipulation2() {
    runTestOk(new PropClass2Recognizer(), new PropClass2(1, 2, 3), "@PropClass2(b:2)@a(1){3}");
  }

  public static abstract class AbstractBase {
    @AutoForm.Name("KEY")
    private String key;

    public AbstractBase() {

    }

    public AbstractBase(String key) {
      this.key = key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof AbstractBase)) return false;
      AbstractBase that = (AbstractBase) o;
      return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key);
    }
  }

  @AutoForm
  public static class Impl1 extends AbstractBase {
    private String value;

    public Impl1() {

    }

    public Impl1(String key, String value) {
      super(key);
      this.value = value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Impl1)) return false;
      if (!super.equals(o)) return false;
      Impl1 impl1 = (Impl1) o;
      return Objects.equals(value, impl1.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }

  @Test
  void testAbstractClassImpl() {
    runTestOk(new Impl1Recognizer(), new Impl1("key", "value"), "@Impl1{KEY:key,value:value}");
  }

  @AutoForm
  public static class Inner {
    private int first;
    private String second;

    public Inner() {

    }

    public Inner(int first, String second) {
      this.first = first;
      this.second = second;
    }

    public void setFirst(int first) {
      this.first = first;
    }

    public void setSecond(String second) {
      this.second = second;
    }

    @Override
    public String toString() {
      return "Inner{" +
          "first=" + first +
          ", second='" + second + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Inner)) return false;
      Inner inner = (Inner) o;
      return first == inner.first && Objects.equals(second, inner.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }
  }

  @AutoForm
  public static class Outer {
    private String node;
    @AutoForm.Kind(FieldKind.Body)
    private Inner inner;

    public Outer() {

    }

    public Outer(String node, Inner inner) {
      this.node = node;
      this.inner = inner;
    }

    public void setNode(String node) {
      this.node = node;
    }

    public void setInner(Inner inner) {
      this.inner = inner;
    }

    @Override
    public String toString() {
      return "Outer{" +
          "node='" + node + '\'' +
          ", inner=" + inner +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Outer)) return false;
      Outer outer = (Outer) o;
      return Objects.equals(node, outer.node) && Objects.equals(inner, outer.inner);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, inner);
    }
  }

  @Test
  void testNested() {
    runTestOk(new OuterRecognizer(), new Outer("node_uri", new Inner(1034, "inside")), "@Outer(node: node_uri) @Inner { first: 1034, second: inside }");
  }

  @AutoForm
  public static class Prop3 {
    @AutoForm.Kind(FieldKind.HeaderBody)
    public int count;
    @AutoForm.Kind(FieldKind.Header)
    public String node;
    @AutoForm.Kind(FieldKind.Header)
    String lane;
    int first;
    String second;

    public Prop3() {

    }

    public Prop3(int count, String node, String lane, int first, String second) {
      this.count = count;
      this.node = node;
      this.lane = lane;
      this.first = first;
      this.second = second;
    }

    public void setLane(String lane) {
      this.lane = lane;
    }

    public void setFirst(int first) {
      this.first = first;
    }

    public void setSecond(String second) {
      this.second = second;
    }

    @Override
    public String toString() {
      return "Prop3{" +
          "count=" + count +
          ", node='" + node + '\'' +
          ", lane='" + lane + '\'' +
          ", first=" + first +
          ", second='" + second + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Prop3)) return false;
      Prop3 prop3 = (Prop3) o;
      return count == prop3.count && first == prop3.first && Objects.equals(node, prop3.node) && Objects.equals(lane, prop3.lane) && Objects.equals(second, prop3.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(count, node, lane, first, second);
    }
  }

  @Test
  void testComplexHeader() {
    runTestOk(new Prop3Recognizer(), new Prop3(6, "node_uri", "lane_uri", -34, "name"), "@Prop3(6, node: node_uri, lane: lane_uri) { first: -34, second: \"name\" }");
    runTestOk(new Prop3Recognizer(), new Prop3(6, "node_uri", "lane_uri", -34, "name"), "@Prop3(6, lane: lane_uri, node: node_uri) { first: -34, second: \"name\" }");
    runTestOk(new Prop3Recognizer(), new Prop3(6, "node_uri", "lane_uri", -34, "name"), "@Prop3({6, lane: lane_uri, node: node_uri}) { first: -34, second: \"name\" }");
  }

  @AutoForm(subTypes = {
      @AutoForm.Type(LaneAddressed.class),
      @AutoForm.Type(HostAddressed.class)
  })
  public static abstract class Envelope {

  }

  @AutoForm(subTypes = @AutoForm.Type(CommandMessage.class))
  public static abstract class LaneAddressed extends Envelope {
    @AutoForm.Name("node")
    private String nodeUri;
    @AutoForm.Name("lane")
    private String laneUri;
    @AutoForm.Kind(FieldKind.Body)
    private Object body;

    public LaneAddressed() {

    }

    public LaneAddressed(String nodeUri, String laneUri, Object body) {
      this.nodeUri = nodeUri;
      this.laneUri = laneUri;
      this.body = body;
    }

    public void setNodeUri(String nodeUri) {
      this.nodeUri = nodeUri;
    }

    public void setLaneUri(String laneUri) {
      this.laneUri = laneUri;
    }

    public void setBody(Object body) {
      this.body = body;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof LaneAddressed)) return false;
      LaneAddressed that = (LaneAddressed) o;
      return Objects.equals(nodeUri, that.nodeUri) && Objects.equals(laneUri, that.laneUri) && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
      return Objects.hash(nodeUri, laneUri, body);
    }

    @Override
    public String toString() {
      return "LaneAddressed{" +
          "nodeUri='" + nodeUri + '\'' +
          ", laneUri='" + laneUri + '\'' +
          ", body='" + body + '\'' +
          '}';
    }
  }

  @AutoForm("command")
  public static class CommandMessage extends LaneAddressed {
    public CommandMessage() {

    }

    public CommandMessage(String nodeUri, String laneUri, Object body) {
      super(nodeUri, laneUri, body);
    }
  }

  @AutoForm(subTypes = @AutoForm.Type(AuthRequest.class))
  public static abstract class HostAddressed extends Envelope {
    @AutoForm.Kind(FieldKind.Body)
    Object body;

    public HostAddressed() {

    }

    public HostAddressed(Object body) {
      this.body = body;
    }

    public void setBody(Object body) {
      this.body = body;
    }
  }

  @AutoForm("auth")
  public static class AuthRequest extends HostAddressed {
    public AuthRequest() {

    }

    public AuthRequest(Object body) {
      super(body);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof AuthRequest)) return false;
      AuthRequest that = (AuthRequest) o;
      return Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
      return Objects.hash(body);
    }

    @Override
    public String toString() {
      return "AuthRequest{" +
          "body='" + body + '\'' +
          '}';
    }
  }

  @Test
  void envelopes() {
    runTestOk(new EnvelopeRecognizer(), new CommandMessage("node_uri", "lane_uri", 13), "@command(node:node_uri,lane:lane_uri){13}");
    runTestOk(new EnvelopeRecognizer(), new AuthRequest(13), "@auth{13}");
  }

  @AutoForm(subTypes = {
      @AutoForm.Type(Base2.class),
      @AutoForm.Type(Con2.class)
  })
  public static abstract class Base {

  }

  @AutoForm(subTypes = {
      @AutoForm.Type(ImpBase2.class)
  })
  public static abstract class Base2 extends Base {

  }

  @AutoForm(subTypes = {@AutoForm.Type(Imp3.class)})
  public static class ImpBase2 extends Base2 {
    String a;

    public ImpBase2() {

    }

    public ImpBase2(String a) {
      this.a = a;
    }

    public void setA(String a) {
      this.a = a;
    }

    @Override
    public String toString() {
      return "ImpBase2{" +
          "a='" + a + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ImpBase2)) return false;
      ImpBase2 impBase2 = (ImpBase2) o;
      return Objects.equals(a, impBase2.a);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a);
    }
  }

  @AutoForm
  public static class Imp3 extends ImpBase2 {
    String c;

    public Imp3() {

    }

    public Imp3(String c) {
      this.c = c;
    }

    public void setC(String c) {
      this.c = c;
    }

    @Override
    public String toString() {
      return "Imp3{" +
          "c='" + c + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Imp3)) return false;
      Imp3 imp3 = (Imp3) o;
      return Objects.equals(c, imp3.c);
    }

    @Override
    public int hashCode() {
      return Objects.hash(c);
    }
  }

  @AutoForm
  public static class Con2 extends Base {
    String b;

    public Con2() {

    }

    public Con2(String b) {
      this.b = b;
    }

    public void setB(String b) {
      this.b = b;
    }

    @Override
    public String toString() {
      return "Con2{" +
          "b='" + b + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Con2)) return false;
      Con2 con2 = (Con2) o;
      return Objects.equals(b, con2.b);
    }

    @Override
    public int hashCode() {
      return Objects.hash(b);
    }
  }

  @Test
  void abstractClasses() {
    runTestOk(new BaseRecognizer(), new Imp3("hello"), "@Imp3{c:hello}");
    runTestOk(new Imp3Recognizer(), new Imp3("hello"), "@Imp3{c:hello}");
    runTestOk(new BaseRecognizer(), new Con2("hello"), "@Con2{b:hello}");
    runTestOk(new Con2Recognizer(), new Con2("hello"), "@Con2{b:hello}");
    runTestOk(new BaseRecognizer(), new ImpBase2("hello"), "@ImpBase2{a:hello}");
    runTestOk(new Base2Recognizer(), new ImpBase2("hello"), "@ImpBase2{a:hello}");
    runTestOk(new ImpBase2Recognizer(), new ImpBase2("hello"), "@ImpBase2{a:hello}");
  }

  @AutoForm(subTypes = {
      @AutoForm.Type(IF2.class),
      @AutoForm.Type(IFAbs.class)
  })
  public interface IF1 {

  }

  @AutoForm(subTypes = @AutoForm.Type(IF2Impl.class))
  public interface IF2 extends IF1 {

  }

  @AutoForm
  public static class IF2Impl implements IF2 {
    @Override
    public boolean equals(Object obj) {
      return obj instanceof IF2Impl;
    }
  }

  @AutoForm(subTypes = @AutoForm.Type(IFAbsImpl.class))
  public static abstract class IFAbs implements IF1 {

  }

  @AutoForm
  public static class IFAbsImpl extends IFAbs {
    @Override
    public boolean equals(Object obj) {
      return obj instanceof IFAbsImpl;
    }
  }

  @Test
  void testInterfaceHierarchy() {
    runTestOk(new IF1Recognizer(), new IFAbsImpl(), "@IFAbsImpl");
    runTestOk(new IF1Recognizer(), new IF2Impl(), "@IF2Impl");
    runTestOk(new IF2Recognizer(), new IF2Impl(), "@IF2Impl");
    runTestOk(new IFAbsRecognizer(), new IFAbsImpl(), "@IFAbsImpl");
  }

  @AutoForm(subTypes = @AutoForm.Type(Mixed.class))
  public static abstract class MixedBase {

  }

  @AutoForm(subTypes = @AutoForm.Type(Mixed.class))
  public interface MixedInterface {

  }

  @AutoForm
  public static class Mixed extends MixedBase implements MixedInterface {
    @Override
    public boolean equals(Object obj) {
      return obj instanceof Mixed;
    }
  }

  @Test
  void testMixedHierarchy() {
    runTestOk(new MixedBaseRecognizer(), new Mixed(), "@Mixed");
    runTestOk(new MixedInterfaceRecognizer(), new Mixed(), "@Mixed");
    runTestOk(new MixedRecognizer(), new Mixed(), "@Mixed");
  }

  @AutoForm(subTypes = {
      @AutoForm.Type(MapUpdate.class),
      @AutoForm.Type(MapRemove.class),
      @AutoForm.Type(MapClear.class),
      @AutoForm.Type(MapTake.class),
      @AutoForm.Type(MapDrop.class),
  })
  public static abstract class MapEnvelope {

  }

  @AutoForm("update")
  public static class MapUpdate extends MapEnvelope {
    @AutoForm.Kind(FieldKind.Header)
    public Object key;
    @AutoForm.Kind(FieldKind.Body)
    public Object value;

    public MapUpdate() {

    }

    public MapUpdate(Object key, Object value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MapUpdate)) return false;
      MapUpdate update = (MapUpdate) o;
      return Objects.equals(key, update.key) && Objects.equals(value, update.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, value);
    }
  }

  @AutoForm("remove")
  public static class MapRemove extends MapEnvelope {
    @AutoForm.Kind(FieldKind.Header)
    public Object key;

    public MapRemove() {

    }

    public MapRemove(Object key) {
      this.key = key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MapRemove)) return false;
      MapRemove mapRemove = (MapRemove) o;
      return Objects.equals(key, mapRemove.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key);
    }
  }

  @AutoForm("clear")
  public static class MapClear extends MapEnvelope {
    @Override
    public boolean equals(Object obj) {
      return obj instanceof MapClear;
    }
  }

  @AutoForm("take")
  public static class MapTake extends MapEnvelope {
    @AutoForm.Kind(FieldKind.HeaderBody)
    public int count;

    public MapTake() {

    }

    public MapTake(int count) {
      this.count = count;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MapTake)) return false;
      MapTake mapTake = (MapTake) o;
      return count == mapTake.count;
    }

    @Override
    public int hashCode() {
      return Objects.hash(count);
    }
  }

  @AutoForm("drop")
  public static class MapDrop extends MapEnvelope {
    @AutoForm.Kind(FieldKind.HeaderBody)
    public int count;

    public MapDrop() {

    }

    public MapDrop(int count) {
      this.count = count;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MapDrop)) return false;
      MapDrop mapDrop = (MapDrop) o;
      return count == mapDrop.count;
    }

    @Override
    public int hashCode() {
      return Objects.hash(count);
    }
  }

  @Test
  void testComplexEnvelope() {
    runTestOk(new MapEnvelopeRecognizer(), new MapUpdate(1, 2), "@update(key:1)2");
    runTestOk(new MapEnvelopeRecognizer(), new MapRemove(1), "@remove(key:1)");
    runTestOk(new MapEnvelopeRecognizer(), new MapClear(), "@clear");
    runTestOk(new MapEnvelopeRecognizer(), new MapTake(13), "@take(13)");
    runTestOk(new MapEnvelopeRecognizer(), new MapDrop(13), "@drop(13)");
  }

  // used to check that all the core types find recognizers correctly
  @AutoForm
  public static final class StdTypes {
    public byte a;
    public short b;
    public int c;
    public float d;
    public boolean e;
    public String f;
    public AtomicBoolean g;
    public AtomicInteger h;
    public AtomicLong i;
    public char j;
    public BigInteger k;
    public BigDecimal l;
  }



}
