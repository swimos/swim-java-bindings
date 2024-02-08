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
 * An interface for defining Recon print strategies.
 */
public interface PrintStrategy {
  /**
   * A compacted Recon print strategy. Useful for reducing the size of the output.
   */
  PrintStrategy COMPACT = new CompactPrintStrategy();

  /**
   * A standard Recon print strategy which writes spaces between attributes, items and slots.
   */
  PrintStrategy STANDARD = new StandardPrintStrategy();

  Padding attrPadding();

  Padding attrBodyPadding();

  Padding startBlock(int items);

  Padding endBlock();

  Padding itemPadding(boolean inRecord);

  Padding slotPadding();
}
