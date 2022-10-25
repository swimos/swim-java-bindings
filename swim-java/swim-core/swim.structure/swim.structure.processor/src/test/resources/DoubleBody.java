import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;

@AutoForm
public class DoubleBody {
  @AutoForm.Kind(FieldKind.Body)
  public int a;
  @AutoForm.Kind(FieldKind.Body)
  public int b;
}