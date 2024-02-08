/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.server.lanes;

import ai.swim.server.lanes.command.CommandLaneView;
import ai.swim.server.lanes.demand.DemandLaneView;
import ai.swim.server.lanes.demandmap.DemandMapLaneView;
import ai.swim.server.lanes.map.MapLaneView;
import ai.swim.server.lanes.supply.SupplyLaneView;
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
   * Returns a new Demand Lane.
   *
   * @param form for encoding and decoding {@code T}
   * @param <T>  the lane's event type.
   * @return a new Demand Lane.
   */
  public static <T> DemandLaneView<T> demandLane(Form<T> form) {
    return new DemandLaneView<>(form);
  }

  /**
   * Returns a new Demand Lane.
   *
   * @param clazz class of {@code T}.
   * @param <T>   the lane's event type.
   * @return a new Demand Lane.
   */
  public static <T> DemandLaneView<T> demandLane(Class<T> clazz) {
    return demandLane(Form.forClass(clazz));
  }

  /**
   * Returns a new Demand Map Lane.
   *
   * @param keyForm   for encoding and decoding {@code K}
   * @param valueForm for encoding and decoding {@code V}
   * @param <K>       the lane's event type.
   * @param <V>       the lane's event type.
   * @return a new Demand Map Lane.
   */
  public static <K, V> DemandMapLaneView<K, V> demandMapLane(Form<K> keyForm, Form<V> valueForm) {
    return new DemandMapLaneView<>(keyForm, valueForm);
  }

  /**
   * Returns a new Demand Map Lane.
   *
   * @param keyClass   class of {@code K}
   * @param valueClass class of {@code V}
   * @param <K>        the lane's event type.
   * @param <V>        the lane's event type.
   * @return a new Demand Map Lane.
   */
  public static <K, V> DemandMapLaneView<K, V> demandMapLane(Class<K> keyClass, Class<V> valueClass) {
    return new DemandMapLaneView<>(Form.forClass(keyClass), Form.forClass(valueClass));
  }


  /**
   * Returns a new Supply Lane.
   *
   * @param form for encoding and decoding {@code T}
   * @param <T>  the lane's event type.
   * @return a new Supply Lane.
   */
  public static <T> SupplyLaneView<T> supplyLane(Form<T> form) {
    return new SupplyLaneView<>(form);
  }

  /**
   * Returns a new Supply Lane.
   *
   * @param clazz class of {@code T}.
   * @param <T>   the lane's event type.
   * @return a new Supply Lane.
   */
  public static <T> SupplyLaneView<T> supplyLane(Class<T> clazz) {
    return supplyLane(Form.forClass(clazz));
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
