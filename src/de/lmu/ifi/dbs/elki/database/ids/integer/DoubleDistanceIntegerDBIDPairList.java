package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Class to store double distance, integer DBID results.
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceIntegerDBIDPairList implements ModifiableDoubleDistanceDBIDList, IntegerDBIDs {
  /**
   * The size
   */
  int size;

  /**
   * Distance values
   */
  DoubleDistanceIntegerDBIDPair[] data;

  /**
   * Constructor.
   */
  public DoubleDistanceIntegerDBIDPairList() {
    super();
    this.data = new DoubleDistanceIntegerDBIDPair[21];
  }

  /**
   * Constructor.
   * 
   * @param size Initial size
   */
  public DoubleDistanceIntegerDBIDPairList(int size) {
    super();
    if (size > 0) {
      size = 21;
    }
    this.data = new DoubleDistanceIntegerDBIDPair[size];
  }

  @Override
  public DoubleDistanceIntegerDBIDListIter iter() {
    return new Itr();
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int q = o.internalGetIndex();
    for (int i = 0; i < size; i++) {
      if (q == data[i].internalGetIndex()) {
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
  public DoubleDistanceIntegerDBIDPair get(int index) {
    return data[index];
  }

  /**
   * Add an entry, consisting of distance and internal index.
   * 
   * @param pair entry
   */
  protected void addInternal(DoubleDistanceIntegerDBIDPair pair) {
    if (size == data.length) {
      DoubleDistanceIntegerDBIDPair[] old = data;
      data = new DoubleDistanceIntegerDBIDPair[(data.length << 1) + 1];
      System.arraycopy(old, 0, data, 0, old.length);
    }
    data[size++] = pair;
  }

  @Override
  @Deprecated
  public void add(DoubleDistance dist, DBIDRef id) {
    add(dist.doubleValue(), id);
  }

  @Override
  public void add(double dist, DBIDRef id) {
    addInternal(new DoubleDistanceIntegerDBIDPair(dist, id.internalGetIndex()));
  }

  @Override
  public void add(DoubleDistanceDBIDPair pair) {
    if (pair instanceof DoubleDistanceIntegerDBIDPair) {
      addInternal((DoubleDistanceIntegerDBIDPair) pair);
    } else {
      addInternal(new DoubleDistanceIntegerDBIDPair(pair.doubleDistance(), pair.internalGetIndex()));
    }
  }

  @Override
  public void clear() {
    Arrays.fill(data, null);
    size = 0;
  }

  @Override
  public void sort() {
    Arrays.sort(data, 0, size, DistanceDBIDResultUtil.distanceComparator());
  }

  /**
   * Reverse the list.
   */
  protected void reverse() {
    for (int i = 0, j = size - 1; i < j; i++, j--) {
      DoubleDistanceIntegerDBIDPair tmpd = data[j];
      data[j] = data[i];
      data[i] = tmpd;
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNList[");
    for (DoubleDistanceDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleDistance()).append(':').append(iter.internalGetIndex());
      iter.advance();
      if (iter.valid()) {
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
  private class Itr implements DoubleDistanceIntegerDBIDListIter {
    int offset = 0;

    @Override
    public boolean valid() {
      return offset < size;
    }

    @Override
    public void advance() {
      ++offset;
    }

    @Override
    public int getOffset() {
      return offset;
    }

    @Override
    public void advance(int count) {
      offset += count;
    }

    @Override
    public void retract() {
      offset--;
    }

    @Override
    public void seek(int off) {
      offset = off;
    }

    @Override
    public int internalGetIndex() {
      return data[offset].internalGetIndex();
    }

    @Override
    public double doubleDistance() {
      return data[offset].doubleDistance();
    }

    @Override
    public DoubleDistanceDBIDPair getDistancePair() {
      return data[offset];
    }

    @Override
    @Deprecated
    public DoubleDistance getDistance() {
      return new DoubleDistance(data[offset].doubleDistance());
    }
  }
}
