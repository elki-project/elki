package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Finalized KNN List.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class KNNList<D extends Distance<D>> extends AbstractCollection<DistanceResultPair<D>> implements KNNResult<D> {
  /**
   * The value of k this was materialized for.
   */
  private final int k;

  /**
   * The actual data array.
   */
  private final Object[] data;

  /**
   * Constructor, to be called from KNNHeap only!
   * 
   * @param heap Calling heap.
   */
  protected KNNList(KNNHeap<D> heap) {
    super();
    this.data = new Object[heap.size()];
    this.k = heap.getK();
    // Get sorted data from heap; but in reverse.
    int i = heap.size();
    while(!heap.isEmpty()) {
      i--;
      assert (i >= 0);
      data[i] = heap.poll();
    }
    assert (data[0] != null);
    assert (heap.size() == 0);
  }

  /**
   * Constructor, to be called from KNNHeap only!
   * 
   * @param list Existing list
   * @param k k
   * @param maxdist infinite distance to return.
   */
  public KNNList(Collection<DistanceResultPair<D>> list, int k, D maxdist) {
    super();
    this.data = list.toArray(new Object[0]);
    this.k = k;
    // TODO: sorting correct?
    Arrays.sort(this.data, Collections.reverseOrder());
    assert (data[0] != null);
  }

  /**
   * Get the K parameter.
   * 
   * @return K
   */
  public int getK() {
    return k;
  }

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   * 
   * @return Maximum distance
   */
  @Override
  public D getKNNDistance() {
    return get(getK() - 1).getDistance();
  }

  /**
   * View as ArrayDBIDs
   * 
   * @return Static DBIDs
   */
  @Override
  public ArrayDBIDs asDBIDs() {
    return KNNUtil.asDBIDs(this);
  }

  /**
   * View as list of distances
   * 
   * @return List of distances view
   */
  @Override
  public List<D> asDistanceList() {
    return KNNUtil.asDistanceList(this);
  }

  /* Make the list unmodifiable! */

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("kNNList[");
    Iterator<DistanceResultPair<D>> iter = this.iterator();
    while(iter.hasNext()) {
      DistanceResultPair<D> pair = iter.next();
      buf.append(pair.getDistance()).append(":").append(pair.getDBID());
      if(iter.hasNext()) {
        buf.append(",");
      }
    }
    buf.append("]");
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  public DistanceResultPair<D> get(int index) {
    return (DistanceResultPair<D>) data[index];
  }

  @Override
  public Iterator<DistanceResultPair<D>> iterator() {
    return new Itr();
  }

  @Override
  public int size() {
    return data.length;
  }

  /**
   * Iterator
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class Itr implements Iterator<DistanceResultPair<D>> {
    /**
     * Cursor position
     */
    private int pos = -1;

    @Override
    public boolean hasNext() {
      return pos + 1 < data.length;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DistanceResultPair<D> next() {
      pos++;
      return (DistanceResultPair<D>) data[pos];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("kNN results are unmodifiable.");
    }
  }
}