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

package ai.swim.structure.processor.model;

public class CoreTypeSpec<T> {
  private final Class<T> clazz;
  private final CoreTypeModel.Kind kind;
  private final T defaultValue;

  public CoreTypeSpec(Class<T> clazz, CoreTypeModel.Kind kind, T defaultValue) {
    this.clazz = clazz;
    this.kind = kind;
    this.defaultValue = defaultValue;
  }

  public Class<T> getClazz() {
    return clazz;
  }

  public CoreTypeModel.Kind getKind() {
    return kind;
  }

  public T getDefaultValue() {
    return defaultValue;
  }
}