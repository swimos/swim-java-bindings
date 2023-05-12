import ai.swim.structure.annotations.AutoForm;

public class BadSubType {
  @AutoForm(subTypes = {
          @AutoForm.Type(Child.class)
  })
  public class Parent {

  }

  public class Child {

  }


}