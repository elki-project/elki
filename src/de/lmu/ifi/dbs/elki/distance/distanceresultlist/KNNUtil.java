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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

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
   * Create an appropriate heap for the distance function.
   * 
   * This will use a double heap if appropriate.
   * 
   * @param df Distance function
   * @param k K value
   * @param <D> distance type
   * @return New heap of size k, appropriate for this distance function.
   */
  @SuppressWarnings("unchecked")
  public static <D extends Distance<D>> KNNHeap<D> newHeap(DistanceFunction<?, D> df, int k) {
    if (DistanceUtil.isDoubleDistanceFunction(df)) {
      return (KNNHeap<D>) new DoubleDistanceKNNHeap(k);
    }
    return new GenericKNNHeap<D>(k);
  }

  /**
   * Create an appropriate heap for the distance function.
   * 
   * This will use a double heap if appropriate.
   * 
   * @param df Distance function
   * @param k K value
   * @param <D> distance type
   * @return New heap of size k, appropriate for this distance function.
   */
  @SuppressWarnings("unchecked")
  public static <D extends Distance<D>> KNNHeap<D> newHeap(DistanceQuery<?, D> df, int k) {
    if (DistanceUtil.isDoubleDistanceFunction(df)) {
      return (KNNHeap<D>) new DoubleDistanceKNNHeap(k);
    }
    return new GenericKNNHeap<D>(k);
  }

  /**
   * Create an appropriate heap for the distance function.
   * 
   * This will use a double heap if appropriate.
   * 
   * @param factory distance prototype
   * @param k K value
   * @param <D> distance type
   * @return New heap of size k, appropriate for this distance type.
   */
  @SuppressWarnings("unchecked")
  public static <D extends Distance<D>> KNNHeap<D> newHeap(D factory, int k) {
    if (factory instanceof DoubleDistance) {
      return (KNNHeap<D>) new DoubleDistanceKNNHeap(k);
    }
    return new GenericKNNHeap<D>(k);
  }

  /**
   * Build a new heap from a given list.
   * 
   * @param exist Existing result
   * @param <D> Distance type
   * @return New heap
   */
  @SuppressWarnings("unchecked")
  public static <D extends Distance<D>> KNNHeap<D> newHeap(KNNResult<D> exist) {
    if (exist instanceof DoubleDistanceKNNList) {
      DoubleDistanceKNNHeap heap = new DoubleDistanceKNNHeap(exist.getK());
      // Insert backwards, as this will produce a proper heap
      for (int i = exist.size() - 1; i >= 0; i--) {
        heap.add((DoubleDistanceDBIDPair) exist.get(i));
      }
      return (KNNHeap<D>) heap;
    } else {
      GenericKNNHeap<D> heap = new GenericKNNHeap<D>(exist.getK());
      // Insert backwards, as this will produce a proper heap
      for (int i = exist.size() - 1; i >= 0; i--) {
        heap.add(exist.get(i));
      }
      return heap;
    }
  }

  /**
   * Sublist of an existing result to contain only the first k elements.
   * 
   * @author Erich Schubert
   * 
   * @param <D> Distance
   */
  protected static class KNNSubList<D extends Distance<D>> implements KNNResult<D> {
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
    private final KNNResult<D> inner;

    /**
     * Constructor.
     * 
     * @param inner Inner instance
     * @param k k value
     */
    public KNNSubList(KNNResult<D> inner, int k) {
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
    public DistanceDBIDResultIter<D> iter() {
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
    private class Itr implements DistanceDBIDResultIter<D> {
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
    }
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
    DistanceDBIDResultIter<D> itr;

    /**
     * Constructor.
     * 
     * @param distanceDBIDResultIter Iterator
     */
    protected DistanceItr(DistanceDBIDResultIter<D> distanceDBIDResultIter) {
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
    final KNNResult<D> parent;

    /**
     * Constructor.
     * 
     * @param parent Owner
     */
    public DistanceView(KNNResult<D> parent) {
      super();
      this.parent = parent;
    }

    @Override
    public D get(int i) {
      return parent.get(i).getDistance();
    }

    @Override
    public Iterator<D> iterator() {
      return new DistanceItr<D>(parent.iter());
    }

    @Override
    public int size() {
      return parent.size();
    }
  }

  /**
   * View as list of distances.
   * 
   * @param list Result to proxy
   * @param <D> distance type
   * @return List of distances view
   */
  public static <D extends Distance<D>> List<D> asDistanceList(KNNResult<D> list) {
    return new DistanceView<D>(list);
  }

  /**
   * Get a subset of the KNN result.
   * 
   * @param list Existing list
   * @param k k
   * @param <D> distance type
   * @return Subset
   */
  public static <D extends Distance<D>> KNNResult<D> subList(KNNResult<D> list, int k) {
    if (k >= list.size()) {
      return list;
    }
    return new KNNSubList<D>(list, k);
  }
}
