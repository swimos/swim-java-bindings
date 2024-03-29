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

package ai.swim.structure.writer.print.strategy;

import ai.swim.structure.writer.print.Padding;

/**
 * A compacted Recon printing strategy that adds no spaces or new lines between items, attributes and slots.
 */
class CompactPrintStrategy implements PrintStrategy {
  @Override
  public Padding attrPadding() {
    return Padding.Simple.NO_SPACE;
  }

  @Override
  public Padding attrBodyPadding() {
    return Padding.Simple.NO_SPACE;
  }

  @Override
  public Padding startBlock(int items) {
    return Padding.Simple.NO_SPACE;
  }

  @Override
  public Padding endBlock() {
    return Padding.Simple.NO_SPACE;
  }

  @Override
  public Padding itemPadding(boolean inRecord) {
    return Padding.Simple.NO_SPACE;
  }

  @Override
  public Padding slotPadding() {
    return Padding.Simple.NO_SPACE;
  }

}
