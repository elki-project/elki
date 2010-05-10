package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Finalized KNN List.
 * 
 * @author Erich Schubert
 *
 * @param <D>
 */
public class KNNList<D extends Distance<D>> extends ArrayList<DistanceResultPair<D>> {
  /**
   * Serial ID
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The value of k this was materialized for.
   */
  private final int k;
  
  /**
   * The maximum distance to return if size() &lt; k
   */
  private final D maxdist;

  /**
   * Constructor, to be called from KNNHeap only!
   * 
   * @param heap Calling heap.
   * @param maxdist infinite distance to return.
   */
  protected KNNList(KNNHeap<D> heap, D maxdist) {
    super(heap.size());
    this.k = heap.getK();
    this.maxdist = maxdist;
    // Get sorted data from heap; but in reverse.
    int i;
    for (i = 0; i < heap.size(); i++) {
      super.add(null);
    }
    while(!heap.isEmpty()) {
      i--;
      assert(i >= 0);
      super.set(i, heap.poll());
    }
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
  public D getKNNDistance() {
    if (size() < getK()) {
      return maxdist;
    }
    return get(size() - 1).getDistance();
  }
  
  /**
   * Get maximum distance in list
   */
  public D getMaximumDistance() {
    if (isEmpty()) {
      return maxdist;
    }
    return get(size() - 1).getDistance();
  }
  
  /**
   * View as ArrayDBIDs
   * 
   * @return Static DBIDs
   */
  public ArrayDBIDs asDBIDs() {
    return new DBIDView();
  }
  
  /**
   * View as list of distances
   * 
   * @return List of distances view
   */
  public List<D> asDistanceList() {
    return new DistanceView();
  }
  
  /* Make the list unmodifiable! */
  
  /** {@inheritDoc} */
  @Override
  public boolean add(@SuppressWarnings("unused") DistanceResultPair<D> e) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void add(@SuppressWarnings("unused") int index, @SuppressWarnings("unused") DistanceResultPair<D> element) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(@SuppressWarnings("unused") Collection<? extends DistanceResultPair<D>> c) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(@SuppressWarnings("unused") int index, @SuppressWarnings("unused") Collection<? extends DistanceResultPair<D>> c) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public DistanceResultPair<D> remove(@SuppressWarnings("unused") int index) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public boolean remove(@SuppressWarnings("unused") Object o) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public DistanceResultPair<D> set(@SuppressWarnings("unused") int index, @SuppressWarnings("unused") DistanceResultPair<D> element) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void trimToSize() {
    throw new UnsupportedOperationException();
  }

  /**
   * Proxy iterator for accessing DBIDs.
   * 
   * @author Erich Schubert
   */
  protected class DBIDItr implements Iterator<DBID> {
    /**
     * The real iterator.
     */
    Iterator<DistanceResultPair<D>> itr;

    @Override
    public boolean hasNext() {
      return itr.hasNext();
    }

    @Override
    public DBID next() {
      return itr.next().getID();
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
  protected class DBIDView extends AbstractList<DBID> implements ArrayDBIDs {
    @Override
    public DBID get(int i) {
      return KNNList.this.get(i).getID();
    }

    @Override
    public Collection<DBID> asCollection() {
      return this;
    }

    @Override
    public Iterator<DBID> iterator() {
      return new DBIDItr();
    }

    @Override
    public int size() {
      return KNNList.this.size();
    }
  }
  
  /**
   * Proxy iterator for accessing DBIDs.
   * 
   * @author Erich Schubert
   */
  protected class DistanceItr implements Iterator<D> {
    /**
     * The real iterator.
     */
    Iterator<DistanceResultPair<D>> itr;

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
  protected class DistanceView extends AbstractList<D> implements List<D> {
    @Override
    public D get(int i) {
      return KNNList.this.get(i).getDistance();
    }

    @Override
    public Iterator<D> iterator() {
      return new DistanceItr();
    }

    @Override
    public int size() {
      return KNNList.this.size();
    }
  }
}