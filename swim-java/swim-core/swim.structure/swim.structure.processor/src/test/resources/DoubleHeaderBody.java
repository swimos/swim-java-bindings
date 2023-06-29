import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;

@AutoForm
public class DoubleHeaderBody {
  @AutoForm.Kind(FieldKind.HeaderBody)
  public int a;
  @AutoForm.Kind(FieldKind.HeaderBody)
  public int b;
}