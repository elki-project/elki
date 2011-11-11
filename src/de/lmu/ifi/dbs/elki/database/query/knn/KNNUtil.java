package de.lmu.ifi.dbs.elki.database.query.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Helper classes for kNN results.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses KNNResult
 */
public final class KNNUtil {
  /**
   * Sublist of an existing result to contain only the first k elements.
   * 
   * @author Erich Schubert
   * 
   * @param <D> Distance
   */
  protected static class KNNSubList<D extends Distance<D>> extends AbstractCollection<DistanceResultPair<D>> implements KNNResult<D> {
    /**
     * Parameter k
     */
    private final int k;

    /**
     * Actual size, including ties
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
        D dist = inner.get(k).getDistance();
        int i = k;
        while(i + 1 < inner.size()) {
          if(dist.compareTo(inner.get(i + 1).getDistance()) < 0) {
            break;
          }
        }
        size = i;
      }
    }

    @Override
    public int getK() {
      return k;
    }

    @Override
    public DistanceResultPair<D> get(int index) {
      assert (index < size) : "Access beyond design size of list.";
      return inner.get(index);
    }

    @Override
    public D getKNNDistance() {
      return inner.get(k).getDistance();
    }

    @Override
    public ArrayDBIDs asDBIDs() {
      return KNNUtil.asDBIDs(this);
    }

    @Override
    public List<D> asDistanceList() {
      return KNNUtil.asDistanceList(this);
    }

    @Override
    public Iterator<DistanceResultPair<D>> iterator() {
      return new Itr();
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
    private class Itr implements Iterator<DistanceResultPair<D>> {
      /**
       * Current position
       */
      private int pos = -1;

      @Override
      public boolean hasNext() {
        return pos + 1 < size;
      }

      @Override
      public DistanceResultPair<D> next() {
        pos++;
        return inner.get(pos);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("kNN results are unmodifiable.");
      }
    }
  }

  /**
   * Proxy iterator for accessing DBIDs.
   * 
   * @author Erich Schubert
   */
  protected static class DBIDItr implements Iterator<DBID> {
    /**
     * The real iterator.
     */
    Iterator<? extends DistanceResultPair<?>> itr;

    /**
     * Constructor.
     */
    protected DBIDItr(Iterator<? extends DistanceResultPair<?>> itr) {
      super();
      this.itr = itr;
    }

    @Override
    public boolean hasNext() {
      return itr.hasNext();
    }

    @Override
    public DBID next() {
      return itr.next().getDBID();
    }

    @Override
    public void remove() {
      itr.remove();
    }
  }

  /**
   * A view on the DBIDs of the result
   * 
   * @author Erich Schubert
   */
  protected static class DBIDView extends AbstractList<DBID> implements ArrayDBIDs {
    /**
     * The true list.
     */
    final KNNResult<?> parent;

    /**
     * Constructor.
     * 
     * @param parent Owner
     */
    public DBIDView(KNNResult<?> parent) {
      super();
      this.parent = parent;
    }

    @Override
    public DBID get(int i) {
      return parent.get(i).getDBID();
    }

    @Override
    public Collection<DBID> asCollection() {
      return this;
    }

    @Override
    public Iterator<DBID> iterator() {
      return new DBIDItr(parent.iterator());
    }

    @Override
    public int size() {
      return parent.size();
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
    Iterator<? extends DistanceResultPair<D>> itr;

    /**
     * Constructor.
     */
    protected DistanceItr(Iterator<? extends DistanceResultPair<D>> itr) {
      super();
      this.itr = itr;
    }

    @Override
    public boolean hasNext() {
      return itr.hasNext();
    }

    @Override
    public D next() {
      return itr.next().getDistance();
    }

    @Override
    public void remove() {
      itr.remove();
    }
  }

  /**
   * A view on the Distances of the result
   * 
   * @author Erich Schubert
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
      return new DistanceItr<D>(parent.iterator());
    }

    @Override
    public int size() {
      return parent.size();
    }
  }

  /**
   * View as ArrayDBIDs
   * 
   * @param list Result to proxy
   * @return Static DBIDs
   */
  public static ArrayDBIDs asDBIDs(KNNResult<?> list) {
    return new DBIDView(list);
  }

  /**
   * View as list of distances
   * 
   * @param list Result to proxy
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
   * @return Subset
   */
  public static <D extends Distance<D>> KNNResult<D> subList(KNNResult<D> list, int k) {
    if(k >= list.size()) {
      return list;
    }
    return new KNNSubList<D>(list, k);
  }
}