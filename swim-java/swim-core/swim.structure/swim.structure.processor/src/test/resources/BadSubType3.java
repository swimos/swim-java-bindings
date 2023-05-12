import ai.swim.structure.annotations.AutoForm;

public class BadSubType3 {
  @AutoForm(subTypes = {
      @AutoForm.Type(Parent.class)
  })
  public abstract class Parent {

  }

}