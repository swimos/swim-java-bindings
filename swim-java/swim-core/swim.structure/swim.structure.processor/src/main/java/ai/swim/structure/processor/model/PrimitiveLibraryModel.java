package ai.swim.structure.processor.model;

import ai.swim.structure.processor.model.mapping.CoreTypeKind;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/**
 * A primitive library model representing either a Character, Byte, Short, Integer, Long, Float, Double, Boolean,
 * String, Number, BigInteger or BigDecimal.
 */
public class PrimitiveLibraryModel extends Model {
  private final CoreTypeKind coreTypeKind;

  private PrimitiveLibraryModel(TypeMirror type, Element element, CoreTypeKind coreTypeKind, PackageElement packageElement) {
    super(type, element, packageElement);
    this.coreTypeKind = coreTypeKind;
  }

  public static PrimitiveLibraryModel from(ProcessingEnvironment environment, TypeMirror typeMirror, Element element, CoreTypeKind coreTypeKind) {
    Elements elementUtils = environment.getElementUtils();
    PackageElement packageElement = elementUtils.getPackageElement(coreTypeKind.getPackageName());
    return new PrimitiveLibraryModel(typeMirror, element, coreTypeKind, packageElement);
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.core(coreTypeKind);
  }

  @Override
  public Object defaultValue() {
    switch (coreTypeKind) {
      case Character:
        return '\u0000';
      case Byte:
        return (byte) 0;
      case Short:
        return (short) 0;
      case Integer:
        return 0;
      case Long:
        return 0L;
      case Float:
        return 0.0f;
      case Double:
        return 0.0d;
      case Boolean:
        return false;
      case String:
        return "";
      case Number:
      case BigInteger:
      case BigDecimal:
        return null;
      default:
        throw new AssertionError("Unhandled primitive type: " + this);
    }
  }

  @Override
  public String toString() {
    return "PrimitiveLibraryModel{" +
            "coreTypeKind=" + coreTypeKind +
            ", type=" + type +
            '}';
  }
}
