package ai.swim.server.lanes;

public interface Log {
  void trace(Object message);

  void debug(Object message);

  void info(Object message);

  void warn(Object message);

  void error(Object message);

  void fail(Object message);
}
