package ai.swim.structure.processor.model;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

public class CoreTypeModel<T> extends Model {
  private final Kind kind;
  private final T defaultValue;

  private CoreTypeModel(TypeMirror type, Element element, Kind kind, T defaultValue, PackageElement packageElement) {
    super(type, element, packageElement);
    this.kind = kind;
    this.defaultValue = defaultValue;
  }

  public static <T> CoreTypeModel<T> from(ProcessingEnvironment environment,
      TypeMirror typeMirror,
      Element element,
      CoreTypeSpec<T> spec) {
    Elements elementUtils = environment.getElementUtils();
    PackageElement packageElement = elementUtils.getPackageElement(spec.getClazz().getPackageName());
    return new CoreTypeModel<>(typeMirror, element, spec.getKind(), spec.getDefaultValue(), packageElement);
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.core(this);
  }

  @Override
  public Object defaultValue() {
    return defaultValue;
  }

  public Kind getKind() {
    return kind;
  }

  public enum Kind {
    Character, Byte, Short, Integer, Long, Float, Double, String, Boolean, Number, BigInteger, BigDecimal,
  }
}


