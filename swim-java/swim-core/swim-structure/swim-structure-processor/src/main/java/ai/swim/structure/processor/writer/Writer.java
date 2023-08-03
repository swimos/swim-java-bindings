package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.InterfaceModel;
import java.io.IOException;

/**
 * An interface for specifying how a model should be written and transformed into another type.
 * <p>
 * Implementations accept models, transform them into a target type, and write them to the {@link javax.annotation.processing.ProcessingEnvironment}'s
 * {@link javax.annotation.processing.Filer}.
 */
public interface Writer {
  /**
   * Write a {@link ClassLikeModel}.
   */
  void writeClass(ClassLikeModel model) throws IOException;

  /**
   * Write a {@link InterfaceModel}.
   */
  void writeInterface(InterfaceModel model) throws IOException;
}
