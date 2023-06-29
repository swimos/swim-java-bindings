import ai.swim.structure.annotations.AutoForm;

@AutoForm
public enum DuplicateTag {
  A,
  @AutoForm.Tag("A")
  B;
}
