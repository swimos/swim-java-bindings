package ai.swim.server.lanes.map;

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.lifecycle.OnClear;
import ai.swim.server.lanes.lifecycle.OnRemove;
import ai.swim.server.lanes.lifecycle.OnUpdate;
import ai.swim.structure.Form;

/**
 * Swim Map Lane.
 * <p>
 * This interface provides methods for setting lifecycle callbacks and updating the state of the backing map data
 * structure.
 *
 * @param <K> the type of the keys contained in the map.
 * @param <V> the type of the values contained in the map.
 */
public interface MapLane<K, V> extends Lane {

  /**
   * Returns the {@link Form} associated with the key type.
   */
  Form<K> keyForm();

  /**
   * Returns the {@link Form} associated with the value type.
   */
  Form<V> valueForm();

  /**
   * Sets the {@link OnUpdate} that will be invoked when an entry is inserted into the map.
   */
  MapLaneView<K, V> onUpdate(OnUpdate<K, V> onUpdate);

  /**
   * Sets the {@link OnRemove} that will be invoked when an entry is removed from the map.
   */
  MapLaneView<K, V> onRemove(OnRemove<K, V> onRemove);

  /**
   * Sets the {@link OnClear} that will be invoked when the map is cleared.
   */
  MapLaneView<K, V> onClear(OnClear onClear);

  /**
   * Invokes the {@link OnUpdate} callback if one has been registered.
   *
   * @param key      that was updated.
   * @param oldValue associated with the key.
   * @param newValue that has been inserted.
   */
  void onUpdate(K key, V oldValue, V newValue);

  /**
   * Invokes the {@link OnRemove} callback if one has been registered.
   *
   * @param key   that was removed.
   * @param value that has been removed.
   */
  void onRemove(K key, V value);

  /**
   * Invokes the {@link OnClear} callback if one has been registered.
   */
  void onClear();

  /**
   * Clears the map and invokes the {@link OnClear} callback if one has been registered.
   */
  void clear();

  /**
   * Inserts an entry into the map and invokes the {@link OnUpdate} callback if one has been registered.
   *
   * @param key   that will be inserted.
   * @param value to associate with the key.
   */
  V update(K key, V value);

  /**
   * Removes an entry from the map and invokes the {@link OnRemove} callback if one has been registered.
   *
   * @param key to remove.
   * @return the value associated with key if one existed
   */
  V remove(K key);

  /**
   * Gets an entry from the map.
   *
   * @param key the key whose associated value is to be returned.
   */
  V get(K key);

}
