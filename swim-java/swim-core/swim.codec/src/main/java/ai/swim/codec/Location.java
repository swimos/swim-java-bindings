package ai.swim.codec;

/**
 * Marker interface denoting an index into a source.
 */
public interface Location {

   static Location of(int line, int column) {
    return new StringLocation(line,column);
  }

}

