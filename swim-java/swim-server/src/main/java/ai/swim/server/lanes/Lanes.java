package ai.swim.server.lanes;

import ai.swim.server.lanes.command.CommandLaneView;
import ai.swim.server.lanes.map.MapLaneView;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.structure.Form;

public class Lanes {
  /**
   * Returns a new Value Lane.
   *
   * @param form for encoding and decoding {@code T}
   * @param <T>  the lane's event type.
   * @return a new Value Lane.
   */
  public static <T> ValueLaneView<T> valueLane(Form<T> form) {
    return new ValueLaneView<>(form);
  }

  /**
   * Returns a new Value Lane.
   *
   * @param clazz class of {@code T}.
   * @param <T>   the lane's event type.
   * @return a new Value Lane.
   */
  public static <T> ValueLaneView<T> valueLane(Class<T> clazz) {
    return valueLane(Form.forClass(clazz));
  }

  /**
   * Returns a new Map Lane.
   *
   * @param keyForm   for encoding and decoding {@code K}
   * @param valueForm for encoding and decoding {@code V}
   * @param <K>       the lane's key type.
   * @param <V>       the lane's value type.
   * @return a new Map Lane.
   */
  public static <K, V> MapLaneView<K, V> mapLane(Form<K> keyForm, Form<V> valueForm) {
    return new MapLaneView<>(keyForm, valueForm);
  }

  /**
   * Returns a new Map Lane.
   *
   * @param keyClass   class of {@code K}.
   * @param valueClass class of {@code V}.
   * @param <K>        the lane's key type.
   * @param <V>        the lane's value type.
   * @return a new Map Lane.
   */
  public static <K, V> MapLaneView<K, V> mapLane(Class<K> keyClass, Class<V> valueClass) {
    return new MapLaneView<>(Form.forClass(keyClass), Form.forClass(valueClass));
  }

  /**
   * Returns a new Command Lane.
   *
   * @param form for encoding and decoding {@code T}
   * @param <T>  the lane's event type.
   * @return a new Command Lane.
   */
  public static <T> CommandLaneView<T> commandLane(Form<T> form) {
    return new CommandLaneView<>(form);
  }

  /**
   * Returns a new Command Lane.
   *
   * @param clazz class of {@code T}.
   * @param <T>   the lane's event type.
   * @return a new Command Lane.
   */
  public static <T> CommandLaneView<T> commandLane(Class<T> clazz) {
    return commandLane(Form.forClass(clazz));
  }

}
