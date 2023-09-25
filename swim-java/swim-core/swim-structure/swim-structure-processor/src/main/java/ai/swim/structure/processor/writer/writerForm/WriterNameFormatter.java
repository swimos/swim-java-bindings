package ai.swim.structure.processor.writer.writerForm;


import ai.swim.structure.processor.writer.NameFormatter;
import javax.lang.model.element.PackageElement;

public class WriterNameFormatter extends NameFormatter {
  public WriterNameFormatter(String name, PackageElement packageElement) {
    super(name, packageElement);
  }

  /**
   * Returns a string with "Writable" suffixed.
   */
  public String writableName(String gen) {
    return String.format("%sWritable", replaceBounds(gen).toLowerCase());
  }

  /**
   * Returns a string with "Writer" suffixed.
   */
  public String writerClassName() {
    return String.format("%sWriter", this.name);
  }

  public String writerClassName(CharSequence name) {
    return String.format("%sWriter", name);
  }

}
