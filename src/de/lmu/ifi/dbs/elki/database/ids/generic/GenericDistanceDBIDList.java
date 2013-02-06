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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Default class to keep a list of distance-object pairs.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class GenericDistanceDBIDList<D extends Distance<D>> implements ModifiableDistanceDBIDList<D> {
  /**
   * Actual storage.
   */
  final ArrayList<DistanceDBIDPair<D>> storage;

  /**
   * Constructor.
   */
  public GenericDistanceDBIDList() {
    super();
    storage = new ArrayList<>();
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity Capacity
   */
  public GenericDistanceDBIDList(int initialCapacity) {
    super();
    storage = new ArrayList<>(initialCapacity);
  }

  @Override
  public void add(D dist, DBIDRef id) {
    storage.add(DBIDFactory.FACTORY.newDistancePair(dist, id));
  }

  /**
   * Add a prepared pair.
   * 
   * @param pair Pair to add
   */
  public void add(DistanceDBIDPair<D> pair) {
    storage.add(pair);
  }

  @Override
  public void sort() {
    DistanceDBIDResultUtil.sortByDistance(storage);
  }

  @Override
  public int size() {
    return storage.size();
  }

  @Override
  public DistanceDBIDPair<D> get(int off) {
    return storage.get(off);
  }

  @Override
  public DistanceDBIDListIter<D> iter() {
    return new Itr();
  }

  @Override
  public boolean contains(DBIDRef o) {
    for(DBIDIter iter = iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(iter, o)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public String toString() {
    return DistanceDBIDResultUtil.toString(this);
  }

  /**
   * Iterator class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class Itr implements DistanceDBIDListIter<D> {
    /**
     * Iterator position.
     */
    int pos = 0;

    @Override
    public int internalGetIndex() {
      return get(pos).internalGetIndex();
    }

    @Override
    public boolean valid() {
      return pos < size();
    }

    @Override
    public void advance() {
      pos++;
    }

    @Override
    public D getDistance() {
      return get(pos).getDistance();
    }

    @Override
    public DistanceDBIDPair<D> getDistancePair() {
      return get(pos);
    }
    
    @Override
    public String toString() {
      return valid() ? getDistancePair().toString() : "null";
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