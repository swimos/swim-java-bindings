package ai.swim.server.lanes.demandmap;

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.lifecycle.OnCue;
import ai.swim.server.lanes.lifecycle.OnCueKey;
import ai.swim.server.lanes.lifecycle.OnSyncKeys;
import ai.swim.structure.Form;
import java.util.Iterator;

/**
 * Swim Demand Map Lane interface.
 *
 * @param <K> the key type this lane produces.
 * @param <V> the value type this lane produces.
 */
public interface DemandMapLane<K, V> extends Lane {
  /**
   * Returns the {@link Form} associated with the key type.
   */
  Form<K> keyForm();

  /**
   * Returns the {@link Form} associated with the value type.
   */
  Form<V> valueForm();

  /**
   * Sets the {@link OnCue} that will be invoked when a value is to be produced.
   */
  DemandMapLaneView<K, V> onCueKey(OnCueKey<K, V> onCueKey);

  /**
   * Sets the {@link OnSyncKeys} that will be invoked when a value is to be produced.
   */
  DemandMapLaneView<K, V> onSyncKeys(OnSyncKeys<K> onSyncKeys);

  /**
   * Invokes the registered {@link OnCueKey} callback. If a callback has been registered and 'null' is returned then a
   * remove operation is propagated.
   */
  V onCueKey(K key);

  /**
   * Invokes the registered {@link OnSyncKeys} callback. If one has not been registered then should return an empty
   * {@link Iterator}.
   */
  Iterator<K> onSyncKeys();

  /**
   * Cues a key.
   */
  void cueKey(K key);

}
