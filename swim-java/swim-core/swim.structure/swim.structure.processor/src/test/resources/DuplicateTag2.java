import ai.swim.structure.annotations.AutoForm;

@AutoForm
public enum DuplicateTag2 {
  @AutoForm.Tag("A")
  A,
  @AutoForm.Tag("A")
  B;
}
