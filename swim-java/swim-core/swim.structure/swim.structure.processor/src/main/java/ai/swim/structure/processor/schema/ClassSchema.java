package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.ClassMap;
import ai.swim.structure.processor.writer.FieldDiscriminate;
import ai.swim.structure.processor.writer.Recognizer;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import java.io.IOException;
import java.util.List;

public class ClassSchema implements Schema {
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

  public List<FieldDiscriminate> discriminate() {
    return this.partitionedFields.discriminate();
  }

  @Override
  public String toString() {
    return "ClassSchema{" +
        "classMap=" + classMap +
        ", partitionedFields=" + partitionedFields +
        '}';
  }

  public String getTag() {
    return this.classMap.getTag();
  }

  public ClassMap getClassMap() {
    return classMap;
  }

  @Override
  public Element root() {
    return classMap.getRoot();
  }

  @Override
  public void write(ScopedContext scopedContext) throws IOException {
    Recognizer.writeRecognizer(this, scopedContext);
  }
}
