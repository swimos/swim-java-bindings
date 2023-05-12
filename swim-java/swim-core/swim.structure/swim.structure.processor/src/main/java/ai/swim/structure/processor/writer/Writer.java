package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.InterfaceModel;

import java.io.IOException;

public interface Writer {
  void writeClass(ClassLikeModel model) throws IOException;

  void writeInterface(InterfaceModel model) throws IOException;
}
