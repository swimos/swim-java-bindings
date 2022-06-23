//package ai.swim.structure.recognizer.reflecting;
//
//import ai.swim.recon.event.ReadEvent;
//import ai.swim.structure.annotations.AutoForm;
//import ai.swim.structure.recognizer.Recognizer;
//import ai.swim.structure.recognizer.proxy.RecognizerProxy;
//import ai.swim.structure.recognizer.std.ScalarRecognizer;
//import ai.swim.structure.recognizer.untyped.UntypedRecognizer;
//
//class ReflectingRecognizerTest {
//
//  @AutoForm
//  public static class GenericClass<A, B> {
//    public Class1<A> a;
//    public Class2<Integer, B> b;
//
//  }
//
//  public static class GenericClassRecognizer<A, B> extends Recognizer<GenericClassRecognizer<A, B>> {
//    private Recognizer<A> aRecognizer;
//    private Recognizer<Class2<Integer, B>> bRecognizer;
//
//    public GenericClassRecognizer() {
//      this.aRecognizer = new UntypedRecognizer<>();
//
//      RecognizerProxy.getInstance().lookupStructural(Class2.class, TypeParameter.of(Integer.class), TypeParameter.none());
//
//      this.bRecognizer = new Class2Recognizer<>(ScalarRecognizer.INTEGER, new UntypedRecognizer<>());
//    }
//
//    @AutoForm.TypedConstructor
//    public GenericClassRecognizer(Recognizer<A> aRecognizer, Recognizer<B> bRecognizer) {
//
//    }
//
//    @Override
//    public Recognizer<GenericClassRecognizer<A, B>> feedEvent(ReadEvent event) {
//      return null;
//    }
//
//    @Override
//    public Recognizer<GenericClassRecognizer<A, B>> reset() {
//      return null;
//    }
//  }
//
//  public static final class Class2<A, B> {
//    public A a;
//    public B b;
//  }
//
//  public static final class Class2Recognizer<A, B> extends Recognizer<Class2<A, B>> {
//    private Recognizer<A> aRecognizer;
//    private Recognizer<B> bRecognizer;
//
//    // todo: if this constructor is called then it should contain either a target (<A>...) recognizer or an untypedrecognizer
//    public Class2Recognizer(Recognizer<A> aRecognizer, Recognizer<B> bRecognizer) {
//      this.aRecognizer = aRecognizer;
//      this.bRecognizer = bRecognizer;
//    }
//
//    @Override
//    public Recognizer<Class2<A, B>> feedEvent(ReadEvent event) {
//      return null;
//    }
//
//    @Override
//    public Recognizer<Class2<A, B>> reset() {
//      return null;
//    }
//  }
//
//}