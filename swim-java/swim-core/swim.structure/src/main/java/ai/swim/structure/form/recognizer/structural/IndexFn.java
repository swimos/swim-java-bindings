package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.recognizer.structural.key.LabelledFieldKey;

@FunctionalInterface
public interface IndexFn {
  Integer selectIndex(LabelledFieldKey key);
}
