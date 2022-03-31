package ai.swim.structure.recognizer.structural;

import ai.swim.structure.recognizer.structural.key.LabelledFieldKey;

@FunctionalInterface
public interface IndexFn {

  Integer selectIndex(LabelledFieldKey key);

}
