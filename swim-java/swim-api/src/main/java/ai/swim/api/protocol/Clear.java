package ai.swim.api.protocol;

import ai.swim.structure.annotations.AutoForm;

@AutoForm
@AutoForm.Tag("clear")
public class Clear extends MapMessage {
  public Clear() {

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  public boolean isClear() {
    return true;
  }
}
