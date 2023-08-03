package ai.swim.structure.processor.writer.recognizerForm;

import ai.swim.structure.processor.model.ModelInspector;
import ai.swim.structure.processor.writer.Context;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

public class RecognizerContext extends Context<RecognizerTypeInitializer, RecognizerNameFormatter> {
  private RecognizerContext(Element root,
      ProcessingEnvironment processingEnvironment,
      String name,
      PackageElement packageElement,
      RecognizerTypeInitializer initializer) {
    super(root, processingEnvironment, initializer, new RecognizerNameFormatter(name, packageElement));
  }

  public static RecognizerContext build(Element root,
      ProcessingEnvironment processingEnvironment,
      ModelInspector inspector,
      String name,
      PackageElement packageElement) {
    RecognizerNameFormatter formatter = new RecognizerNameFormatter(name, packageElement);
    RecognizerTypeInitializer initializer = new RecognizerTypeInitializer(processingEnvironment, formatter, inspector);
    return new RecognizerContext(root, processingEnvironment, name, packageElement, initializer);
  }
}
