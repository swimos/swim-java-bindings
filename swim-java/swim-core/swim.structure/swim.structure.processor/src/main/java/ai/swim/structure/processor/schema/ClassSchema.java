package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.model.ClassLikeModel;

public class ClassSchema extends AbstractSchema {

  ClassSchema(ClassLikeModel model, PartitionedFields partitionedFields) {
    super(model, partitionedFields);
  }

  public String getTag() {
    return this.model.getTag();
  }

}
