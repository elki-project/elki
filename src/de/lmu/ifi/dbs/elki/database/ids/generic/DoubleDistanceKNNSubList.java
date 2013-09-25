package de.lmu.ifi.dbs.elki.database.ids.generic;

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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Sublist of an existing result to contain only the first k elements.
 * 
 * TOOD: can be optimized slightly better.
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceKNNSubList implements DoubleDistanceKNNList {
  /**
   * Parameter k.
   */
  private final int k;

  /**
   * Actual size, including ties.
   */
  private final int size;

  /**
   * Wrapped inner result.
   */
  private final DoubleDistanceKNNList inner;

  /**
   * Constructor.
   * 
   * @param inner Inner instance
   * @param k k value
   */
  public DoubleDistanceKNNSubList(DoubleDistanceKNNList inner, int k) {
    this.inner = inner;
    this.k = k;
    // Compute list size
    {
      DoubleDistanceDBIDPair dist = inner.get(k);
      int i = k;
      while (i + 1 < inner.size()) {
        if (dist.compareByDistance(inner.get(i + 1)) < 0) {
          break;
        }
        i++;
      }
      size = i;
    }
  }

  @Override
  public int getK() {
    return k;
  }

  @Override
  public DoubleDistanceDBIDPair get(int index) {
    assert (index < size) : "Access beyond design size of list.";
    return inner.get(index);
  }

  @Override
  @Deprecated
  public DoubleDistance getKNNDistance() {
    return inner.get(k).getDistance();
  }

  @Override
  public double doubleKNNDistance() {
    return inner.get(k).doubleDistance();
  }

  @Override
  public DoubleDistanceDBIDListIter iter() {
    return new Itr();
  }

  @Override
  public boolean contains(DBIDRef o) {
    for (DBIDIter iter = iter(); iter.valid(); iter.advance()) {
      if (DBIDUtil.equal(iter, o)) {
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

  /**
   * Iterator for the sublist.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class Itr implements DoubleDistanceDBIDListIter {
    /**
     * Current position.
     */
    private int pos = 0;

    @Override
    public boolean valid() {
      return pos < size;
    }

    @Override
    public void advance() {
      pos++;
    }

    @Override
    @Deprecated
    public DoubleDistance getDistance() {
      return inner.get(pos).getDistance();
    }

    @Override
    public double doubleDistance() {
      return inner.get(pos).doubleDistance();
    }

    @Override
    public DoubleDistanceDBIDPair getDistancePair() {
      return inner.get(pos);
    }

    @Override
    public int internalGetIndex() {
      return inner.get(pos).internalGetIndex();
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public void advance(int count) {
      pos += count;
    }

    @Override
    public void retract() {
      --pos;
    }

    @Override
    public void seek(int off) {
      pos = off;
    }
  }
}
