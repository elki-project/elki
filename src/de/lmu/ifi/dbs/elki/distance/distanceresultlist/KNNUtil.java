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

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.generic.KNNSubList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Helper classes for kNN results.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.has KNNResult oneway - - «processes»
 * @apiviz.has KNNHeap oneway - - «creates»
 * @apiviz.has DistanceView
 * @apiviz.has KNNSubList
 */
public final class KNNUtil {
  /**
   * Fake constructor: do not instantiate.
   */
  private KNNUtil() {
    // Empty.
  }

  /**
   * A view on the Distances of the result.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf DistanceItr
   */
  protected static class DistanceView<D extends Distance<D>> extends AbstractList<D> implements List<D> {
    /**
     * The true list.
     */
    final KNNList<D> parent;

    /**
     * Constructor.
     * 
     * @param parent Owner
     */
    public DistanceView(KNNList<D> parent) {
      super();
      this.parent = parent;
    }

    @Override
    public D get(int i) {
      return parent.get(i).getDistance();
    }

    @Override
    public Iterator<D> iterator() {
      return new DistanceItr<>(parent.iter());
    }

    @Override
    public int size() {
      return parent.size();
    }

    /**
     * Proxy iterator for accessing DBIDs.
     * 
     * @author Erich Schubert
     */
    protected static class DistanceItr<D extends Distance<D>> implements Iterator<D> {
      /**
       * The real iterator.
       */
      DistanceDBIDListIter<D> itr;
    
      /**
       * Constructor.
       * 
       * @param distanceDBIDResultIter Iterator
       */
      protected DistanceItr(DistanceDBIDListIter<D> distanceDBIDResultIter) {
        super();
        this.itr = distanceDBIDResultIter;
      }
    
      @Override
      public boolean hasNext() {
        return itr.valid();
      }
    
      @Override
      public D next() {
        D dist = itr.getDistance();
        itr.advance();
        return dist;
      }
    
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
  }

  /**
   * View as list of distances.
   * 
   * @param list Result to proxy
   * @param <D> distance type
   * @return List of distances view
   */
  public static <D extends Distance<D>> List<D> asDistanceList(KNNList<D> list) {
    return new DistanceView<>(list);
  }

  /**
   * Get a subset of the KNN result.
   * 
   * @param list Existing list
   * @param k k
   * @param <D> distance type
   * @return Subset
   */
  public static <D extends Distance<D>> KNNList<D> subList(KNNList<D> list, int k) {
    if (k >= list.size()) {
      return list;
    }
    return new KNNSubList<>(list, k);
  }
}
