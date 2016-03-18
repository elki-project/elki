package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListMIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;

/**
 * Class to store double distance, integer DBID results.
 *
 * Currently unused. Needs benchmarking.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
class DoubleIntegerDBIDPairList implements ModifiableDoubleDBIDList, IntegerDBIDs {
  /**
   * The size
   */
  int size;

  /**
   * Distance values
   */
  DoubleIntegerDBIDPair[] data;

  /**
   * Constructor.
   */
  protected DoubleIntegerDBIDPairList() {
    super();
    this.data = new DoubleIntegerDBIDPair[21];
  }

  /**
   * Constructor.
   *
   * @param size Initial size
   */
  protected DoubleIntegerDBIDPairList(int size) {
    super();
    if(size > 0) {
      size = 21;
    }
    this.data = new DoubleIntegerDBIDPair[size];
  }

  @Override
  public Itr iter() {
    return new Itr();
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int q = o.internalGetIndex();
    for(int i = 0; i < size; i++) {
      if(q == data[i].internalGetIndex()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public DoubleIntegerDBIDPair get(int index) {
    return data[index];
  }

  /**
   * Add an entry, consisting of distance and internal index.
   *
   * @param pair entry
   */
  protected void addInternal(DoubleIntegerDBIDPair pair) {
    if(size == data.length) {
      DoubleIntegerDBIDPair[] old = data;
      data = new DoubleIntegerDBIDPair[(data.length << 1) + 1];
      System.arraycopy(old, 0, data, 0, old.length);
    }
    data[size++] = pair;
  }

  @Override
  public void add(double dist, DBIDRef id) {
    addInternal(new DoubleIntegerDBIDPair(dist, id.internalGetIndex()));
  }

  @Override
  public void add(DoubleDBIDPair pair) {
    if(pair instanceof DoubleIntegerDBIDPair) {
      addInternal((DoubleIntegerDBIDPair) pair);
    }
    else {
      addInternal(new DoubleIntegerDBIDPair(pair.doubleValue(), pair.internalGetIndex()));
    }
  }

  @Override
  public void clear() {
    Arrays.fill(data, null);
    size = 0;
  }

  @Override
  public void sort() {
    Arrays.sort(data, 0, size);
  }

  /**
   * Reverse the list.
   */
  protected void reverse() {
    for(int i = 0, j = size - 1; i < j; i++, j--) {
      DoubleIntegerDBIDPair tmpd = data[j];
      data[j] = data[i];
      data[i] = tmpd;
    }
  }

  @Override
  public void remove(int p) {
    if(p >= size) {
      throw new ArrayIndexOutOfBoundsException(p);
    }
    if(p < --size) {
      System.arraycopy(data, p + 1, data, p, size - p);
    }
    data[size] = null;
  }

  @Override
  public void removeSwap(int p) {
    if(p >= size) {
      throw new ArrayIndexOutOfBoundsException(p);
    }
    if(--size > 0) {
      data[p] = data[size];
    }
    data[size] = null;
  }

  @Override
  public void swap(int i, int j) {
    final DoubleIntegerDBIDPair tmp = data[i];
    data[i] = data[j];
    data[j] = tmp;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNList[");
    for(DoubleDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
      iter.advance();
      if(iter.valid()) {
        buf.append(',');
      }
    }
    buf.append(']');
    return buf.toString();
  }

  /**
   * List iterator.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private class Itr implements DoubleDBIDListMIter, IntegerDBIDArrayIter {
    int pos = 0;

    @Override
    public boolean valid() {
      return pos < size && pos >= 0;
    }

    @Override
    public Itr advance() {
      ++pos;
      return this;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public Itr advance(int count) {
      pos += count;
      return this;
    }

    @Override
    public Itr retract() {
      pos--;
      return this;
    }

    @Override
    public Itr seek(int off) {
      pos = off;
      return this;
    }

    @Override
    public int internalGetIndex() {
      return data[pos].internalGetIndex();
    }

    @Override
    public double doubleValue() {
      return data[pos].doubleValue();
    }

    @Override
    public void setDBID(DBIDRef ref) {
      data[pos].id = ref.internalGetIndex();
    }

    @Override
    public void setDouble(double value) {
      data[pos].value = value;
    }

    @Override
    public DoubleDBIDPair getPair() {
      return data[pos];
    }

    @Override
    public void remove() {
      DoubleIntegerDBIDPairList.this.remove(pos);
    }
  }
}
