package de.lmu.ifi.dbs.elki.database.ids.generic;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

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

/**
 * Sublist of an existing result to contain only the first k elements.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance
 */
public class KNNSubList<D extends Distance<D>> implements KNNList<D> {
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
  private final KNNList<D> inner;

  /**
   * Constructor.
   * 
   * @param inner Inner instance
   * @param k k value
   */
  public KNNSubList(KNNList<D> inner, int k) {
    this.inner = inner;
    this.k = k;
    // Compute list size
    // TODO: optimize for double distances.
    {
      DistanceDBIDPair<D> dist = inner.get(k);
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
  public DistanceDBIDPair<D> get(int index) {
    assert (index < size) : "Access beyond design size of list.";
    return inner.get(index);
  }

  @Override
  public D getKNNDistance() {
    return inner.get(k).getDistance();
  }

  @Override
  public DistanceDBIDListIter<D> iter() {
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
  private class Itr implements DistanceDBIDListIter<D> {
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
    public D getDistance() {
      return inner.get(pos).getDistance();
    }

    @Override
    public DistanceDBIDPair<D> getDistancePair() {
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
      pos -= count;
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
