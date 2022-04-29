package ai.swim.structure.reflect.clazz;

@FunctionalInterface
public interface ConstructorLambda<O> {
  O newInstance();
}