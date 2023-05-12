import ai.swim.structure.annotations.AutoForm;

public class DuplicateField {
  @AutoForm(subTypes = {
      @AutoForm.Type(Child.class)
  })
  public static class Parent {
    public String field;
  }

  @AutoForm
  public static class Child extends Parent {
    public String field;
  }


}