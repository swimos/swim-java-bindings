package ai.swim.codec;

public interface Offset<O> {

  O output();

  int offset();

}
