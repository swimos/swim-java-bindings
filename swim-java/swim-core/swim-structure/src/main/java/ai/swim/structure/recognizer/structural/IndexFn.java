package ai.swim.structure.recognizer.structural;


@FunctionalInterface
public interface IndexFn<Key> {

  Integer selectIndex(Key key);

}
