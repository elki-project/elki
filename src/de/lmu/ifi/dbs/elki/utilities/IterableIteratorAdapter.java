package de.lmu.ifi.dbs.elki.utilities;

import java.util.Iterator;

public final class IterableIteratorAdapter<T> implements IterableIterator<T> {
  /**
   * Parent Iterable
   */
  Iterable<T> parent = null;
  /**
   * Parent Iterator 
   */
  Iterator<T> iter = null;
  
  /**
   * Constructor from an Iterable (preferred).
   * @param parent Iterable parent
   */
  public IterableIteratorAdapter(Iterable<T> parent) {
    this.parent = parent;
    assert(parent != null);
  }

  /**
   * Constructor from an Iterator.
   * 
   * If possible, wrap an Iterable object.
   * @param iter Iterator
   */
  public IterableIteratorAdapter(Iterator<T> iter) {
    this.iter = iter;
    assert(iter != null);
  }

  @Override
  public Iterator<T> iterator() {
    if (parent == null) {
      return this;
    }
    return parent.iterator();
  }

  @Override
  public boolean hasNext() {
    if (iter == null) {
      iter = parent.iterator();
    }
    return iter.hasNext();
  }

  @Override
  public T next() {
    if (iter == null) {
      iter = parent.iterator();
    }
    return iter.next();
  }

  @Override
  public void remove() {
    if (iter == null) {
      iter = parent.iterator();
    }
    iter.remove();
  }
}
