// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.value;

import java.util.Arrays;

public class Record extends Value {
  private static final Attr[] EMPTY_ATTR_DATA = {};
  private static final Item[] EMPTY_ITEM_DATA = {};
  private static final int DEFAULT_CAPACITY = 10;
  private static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

  private Attr[] attrs;
  private Item[] items;
  private int attrCount;
  private int itemCount;

  public Record(int numAttrs, int numItems) {
    this.attrs = numAttrs == 0 ? EMPTY_ATTR_DATA : new Attr[numAttrs];
    this.items = numAttrs == 0 ? EMPTY_ITEM_DATA : new Item[numItems];
    this.attrCount = 0;
    this.itemCount = 0;
  }

  public void pushItem(Value value) {
    if (itemCount == items.length) {
      growItems(itemCount + 1);
    }
    items[itemCount++] = Item.of(value);
  }

  public void pushItem(Value key, Value value) {
    if (itemCount == items.length) {
      growItems(itemCount + 1);
    }
    items[itemCount++] = Item.of(key, value);
  }

  public void pushAttr(Text key, Value value) {
    if (attrCount == attrs.length) {
      growAttrs(attrCount + 1);
    }
    attrs[attrCount++] = new Attr(key, value);
  }

  private void growItems(int by) {
    int oldCapacity = items.length;
    if (oldCapacity > 0 || items != EMPTY_ITEM_DATA) {
      int newCapacity = newArrayLength(oldCapacity, by - oldCapacity, oldCapacity >> 1);
      items = Arrays.copyOf(items, newCapacity);
    } else {
      items = new Item[Math.max(DEFAULT_CAPACITY, by)];
    }
  }

  private void growAttrs(int by) {
    int oldCapacity = attrs.length;
    if (oldCapacity > 0 || attrs != EMPTY_ATTR_DATA) {
      int newCapacity = newArrayLength(oldCapacity, by - oldCapacity, oldCapacity >> 1);
      attrs = Arrays.copyOf(attrs, newCapacity);
    } else {
      attrs = new Attr[Math.max(DEFAULT_CAPACITY, by)];
    }
  }

  private static int newArrayLength(int old, int minGrowth, int growthFactor) {
    int prefLength = old + Math.max(minGrowth, growthFactor);
    if (0 < prefLength && prefLength <= MAX_LENGTH) {
      return prefLength;
    } else {
      int minLength = old + minGrowth;
      if (minLength < 0) {
        throw new OutOfMemoryError(String.format("Array overflow: %s + %s", old, growthFactor));
      } else {
        return Math.max(minLength, MAX_LENGTH);
      }
    }
  }

  @Override
  public String toString() {
    return "Record{" +
        "attrs=" + Arrays.toString(attrs) +
        ", items=" + Arrays.toString(items) +
        ", attrCount=" + attrCount +
        ", itemCount=" + itemCount +
        '}';
  }
}
