package ai.swim.structure.processor.writer.writerForm;

import ai.swim.structure.processor.writer.Context;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

public class WriterContext extends Context<WriterTypeInitializer, WriterNameFormatter> {
  private WriterContext(Element root,
      ProcessingEnvironment processingEnvironment,
      String name,
      PackageElement packageElement,
      WriterTypeInitializer initializer) {
    super(root, processingEnvironment, initializer, new WriterNameFormatter(name, packageElement));
  }

  public static WriterContext build(Element root,
      ProcessingEnvironment processingEnvironment,
      String name,
      PackageElement packageElement) {
    WriterNameFormatter formatter = new WriterNameFormatter(name, packageElement);
    WriterTypeInitializer initializer = new WriterTypeInitializer(processingEnvironment, formatter);
    return new WriterContext(root, processingEnvironment, name, packageElement, initializer);
  }
}
