package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.Comparator;

/**
 * Heap class that is bounded in size from the top.
 * It will keep the bottom {@code k} Elements only.
 * 
 * @author Erich Schubert
 *
 * @param <E> Element type. Should be {@link Comparable} or a {@link Comparator} needs to be given.
 */
public class TopBoundedHeap<E> extends Heap<E> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Maximum size, may be 0 or below for unbounded heaps
   */
  protected int maxsize;

  /**
   * Constructor with maximum size only.
   * 
   * @param maxsize Maximum size
   */
  public TopBoundedHeap(int maxsize) {
    this(maxsize, null);
  }

  /**
   * Constructor with maximum size and {@link Comparator}.
   * 
   * @param maxsize Maximum size
   * @param comparator Comparator
   */
  public TopBoundedHeap(int maxsize, Comparator<? super E> comparator) {
    super(maxsize + 1, comparator);
    this.maxsize = maxsize;
    assert(maxsize > 0);
  }

  @Override
  public boolean offer(E e) {
    // NOTE: we deliberately call super methods here!
    // to have the handleOverflow method called consistently.
    
    // don't add if we hit maxsize and are worse
    if (super.size() >= maxsize) {
      if (super.compareExternal(e, 0) < 0) {
        // while we did not change, this still was "successful".
        return true;
      }
    }
    boolean result = super.offer(e);
    // purge unneeded entry(s)
    while (super.size() > maxsize) {
      handleOverflow(super.poll());
    }
    return result;
  }

  /**
   * Handle an overflow in the structure.
   * This function can be overridden to get overflow treatment.
   * 
   * @param e Overflowing element.
   */
  protected void handleOverflow(E e) {
    // discard extra element
  }
}
