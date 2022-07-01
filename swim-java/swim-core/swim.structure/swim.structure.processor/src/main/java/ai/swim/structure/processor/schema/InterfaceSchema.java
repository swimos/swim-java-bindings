package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.InterfaceMap;
import ai.swim.structure.processor.writer.recognizer.PolymorphicRecognizer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import java.io.IOException;

import static ai.swim.structure.processor.writer.Lookups.RECOGNIZER_PROXY;

public class InterfaceSchema implements Schema {
  private final InterfaceMap interfaceMap;

  public InterfaceSchema(InterfaceMap interfaceMap) {
    this.interfaceMap = interfaceMap;
  }

  public static Schema fromMap(InterfaceMap interfaceMap) {
    return new InterfaceSchema(interfaceMap);
  }

  @Override
  public Element root() {
    return interfaceMap.root();
  }

  @Override
  public void write(ScopedContext scopedContext) throws IOException {
    TypeSpec typeSpec = PolymorphicRecognizer.buildPolymorphicRecognizer(interfaceMap.getSubTypes(), scopedContext)
        .build();
    JavaFile javaFile = JavaFile.builder(interfaceMap.getDeclaredPackage().getQualifiedName().toString(), typeSpec)
        .addStaticImport(ClassName.bestGuess(RECOGNIZER_PROXY), "getProxy")
        .build();

    javaFile.writeTo(scopedContext.getProcessingEnvironment().getFiler());
  }
}
