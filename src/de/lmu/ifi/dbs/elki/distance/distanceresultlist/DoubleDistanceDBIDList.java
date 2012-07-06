package de.lmu.ifi.dbs.elki.distance.distanceresultlist;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Collections;

import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Default class to keep a list of distance-object pairs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DoubleDistanceDBIDPair
 * @apiviz.has DoubleDistanceDBIDResultIter
 */
public class DoubleDistanceDBIDList implements ModifiableDistanceDBIDResult<DoubleDistance> {
  /**
   * Actual storage
   */
  final ArrayList<DoubleDistanceDBIDPair> storage;
  
  /**
   * Constructor.
   */
  public DoubleDistanceDBIDList() {
    super();
    storage = new ArrayList<DoubleDistanceDBIDPair>();
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity Capacity
   */
  public DoubleDistanceDBIDList(int initialCapacity) {
    super();
    storage = new ArrayList<DoubleDistanceDBIDPair>(initialCapacity);
  }
  
  /**
   * Add an element.
   * 
   * @deprecated Pass a double value instead.
   * 
   * @param dist Distance
   * @param id ID
   */
  @Override
  @Deprecated
  public void add(DoubleDistance dist, DBIDRef id) {
    storage.add(DBIDFactory.FACTORY.newDistancePair(dist.doubleValue(), id));
  }

  /**
   * Add an element.
   * 
   * @param dist Distance
   * @param id ID
   */
  public void add(double dist, DBIDRef id) {
    storage.add(DBIDFactory.FACTORY.newDistancePair(dist, id));
  }

  /**
   * Add an element.
   * 
   * @param pair Pair to add
   */
  public void add(DoubleDistanceDBIDPair pair) {
    storage.add(pair);
  }

  @Override
  public void sort() {
    Collections.sort(storage, DistanceDBIDResultUtil.distanceComparator());
  }

  @Override
  public int size() {
    return storage.size();
  }

  @Override
  public DoubleDistanceDBIDPair get(int off) {
    return storage.get(off);
  }

  @Override
  public DoubleDistanceDBIDResultIter iter() {
    return new Iter();
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
  protected class Iter implements DoubleDistanceDBIDResultIter {
    /**
     * Iterator position
     */
    int pos = 0;

    @Override
    public DBIDRef deref() {
      return get(pos);
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
    @Deprecated
    public DoubleDistance getDistance() {
      return get(pos).getDistance();
    }

    @Override
    public double doubleDistance() {
      return get(pos).doubleDistance();
    }

    @Override
    public DoubleDistanceDBIDPair getDistancePair() {
      return get(pos);
    }
    
    @Override
    public String toString() {
      return valid() ? getDistancePair().toString() : "null";
    }
  }
}