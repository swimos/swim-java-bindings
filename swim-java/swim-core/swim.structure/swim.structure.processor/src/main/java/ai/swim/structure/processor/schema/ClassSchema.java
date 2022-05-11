package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.inspect.ClassMap;

import javax.lang.model.element.PackageElement;

public class ClassSchema {
  private final ClassMap classMap;
  private final PartitionedFields partitionedFields;

  private ClassSchema(ClassMap classMap, PartitionedFields partitionedFields) {
    this.classMap = classMap;
    this.partitionedFields = partitionedFields;
  }

  public static ClassSchema fromMap(ClassMap classMap) {
    return new ClassSchema(classMap, PartitionedFields.buildFrom(classMap.getFieldModels()));
  }

  public PackageElement getDeclaredPackage() {
    return this.classMap.getDeclaredPackage();
  }

  public PartitionedFields getPartitionedFields() {
    return partitionedFields;
  }

  @Override
  public String toString() {
    return "ClassSchema{" +
        "classMap=" + classMap +
        ", partitionedFields=" + partitionedFields +
        '}';
  }

  public String getJavaClassName() {
    return this.classMap.getJavaClassName();
  }

  public String getTag() {
    return this.classMap.getTag();
  }

  public String getRecognizerName() {
    return this.classMap.recognizerName();
  }
}
