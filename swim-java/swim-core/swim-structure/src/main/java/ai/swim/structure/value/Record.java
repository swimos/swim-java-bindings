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

import java.util.*;

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

  private Record(Attr[] attrs, int attrCount, Item[] items, int itemCount) {
    this.attrs = attrs;
    this.items = items;
    this.attrCount = attrCount;
    this.itemCount = itemCount;
  }

  public static Record of(List<Attr> attrs, List<Item> items) {
    Attr[] attrArr = new Attr[attrs.size()];
    Item[] itemArr = new Item[items.size()];

    attrs.toArray(attrArr);
    items.toArray(itemArr);

    return new Record(attrArr, attrArr.length, itemArr, itemArr.length);
  }

  public static Record ofItems(List<Item> items) {
    Item[] itemArr = new Item[items.size()];
    items.toArray(itemArr);

    return new Record(EMPTY_ATTR_DATA, 0, itemArr, itemArr.length);
  }

  public static Record ofAttrs(List<Attr> attrs) {
    Attr[] attrArr = new Attr[attrs.size()];
    attrs.toArray(attrArr);

    return new Record(attrArr, attrArr.length, EMPTY_ITEM_DATA, 0);
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

  public int getAttrCount() {
    return attrCount;
  }

  public int getItemCount() {
    return itemCount;
  }

  public Attr[] getAttrs() {
    return attrs;
  }

  public Item[] getItems() {
    return items;
  }

  public void pushItem(Value value) {
    if (itemCount == items.length) {
      growItems(itemCount + 1);
    }
    items[itemCount++] = Item.of(value);
  }

  public void pushItem(Item value) {
    if (itemCount == items.length) {
      growItems(itemCount + 1);
    }
    items[itemCount++] = value;
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

  public void pushAttr(Attr attr) {
    if (attrCount == attrs.length) {
      growAttrs(attrCount + 1);
    }
    attrs[attrCount++] = attr;
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

  @Override
  public String toString() {
    return "Record{" +
        "attrs=" + Arrays.toString(attrs) +
        ", items=" + Arrays.toString(items) +
        ", attrCount=" + attrCount +
        ", itemCount=" + itemCount +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Record record = (Record) o;

    return
        attrCount == record.attrCount && itemCount == record.itemCount
            && Arrays.equals(attrs, 0, attrCount, record.attrs, 0, record.attrCount)
            && Arrays.equals(items, 0, itemCount, record.items, 0, record.itemCount);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(attrCount, itemCount);
    result = 31 * result + Arrays.hashCode(attrs);
    result = 31 * result + Arrays.hashCode(items);
    return result;
  }

  private void clear() {
    attrs = EMPTY_ATTR_DATA;
    items = EMPTY_ITEM_DATA;
    attrCount = 0;
    itemCount = 0;
  }

  public Attr getAttr(int idx) {
    if (idx >= attrCount) {
      throw new IndexOutOfBoundsException();
    } else {
      return attrs[idx];
    }
  }

  public Item getItem(int idx) {
    if (idx >= itemCount) {
      throw new IndexOutOfBoundsException();
    } else {
      return items[idx];
    }
  }

  @Override
  public boolean isRecord() {
    return true;
  }

  public static class Builder {
    private ArrayList<Attr> attrs;
    private ArrayList<Item> items;

    public Builder() {
      this.attrs = new ArrayList<>();
      this.items = new ArrayList<>();
    }

    public Builder(int attrCount, int itemCount) {
      this.attrs = new ArrayList<>(attrCount);
      this.items = new ArrayList<>(itemCount);
    }

    public int attrCount() {
      return attrs.size();
    }

    public int itemCount() {
      return items.size();
    }

    public void pushItem(Value value) {
      items.add(Item.of(value));
    }

    public void pushItem(Item value) {
      items.add(value);
    }

    public Attr popAttr() {
      if (attrs.isEmpty()) {
        return null;
      } else {
        return attrs.remove(attrs.size() - 1);
      }
    }

    public Item popItem() {
      if (items.isEmpty()) {
        return null;
      } else {
        return items.remove(items.size() - 1);
      }
    }

    public void pushItem(Value key, Value value) {
      items.add(Item.of(key, value));
    }

    public void pushAttr(Text key, Value value) {
      attrs.add(new Attr(key, value));
    }

    public void pushAttr(Attr attr) {
      attrs.add(attr);
    }

    public void reserveAttrs(int numAttrs) {
      attrs.ensureCapacity(numAttrs);
    }

    public void reserveItems(int numItems) {
      items.ensureCapacity(numItems);
    }

    public Value buildDelegate(Value to) {
      if (!items.isEmpty()) {
        throw new IllegalStateException("Body already assigned");
      } else if (attrs.isEmpty()) {
        return Record.of(Collections.emptyList(), List.of(Item.of(to)));
      }

      if (to.isRecord()) {
        Record theirs = (Record) to;

        attrs.addAll(Arrays.asList(theirs.attrs).subList(0, theirs.attrCount));
        items.addAll(Arrays.asList(theirs.items).subList(0, theirs.itemCount));

        theirs.clear();

        return Record.of(attrs, items);
      } else {
        return Record.of(attrs, List.of(Item.of(to)));
      }
    }

    public Record build() {
      Record record = Record.of(attrs, items);

      this.attrs = new ArrayList<>(0);
      this.items = new ArrayList<>(0);

      return record;
    }

    public boolean isEmpty() {
      return attrs.isEmpty() && items.isEmpty();
    }

    @Override
    public String toString() {
      return "Record.Builder{" +
          "attrs=" + attrs +
          ", items=" + items +
          '}';
    }


  }

}
