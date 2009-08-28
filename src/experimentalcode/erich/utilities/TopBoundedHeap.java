package experimentalcode.erich.utilities;

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
  // FIXME: ties are not handled correctly yet.
  
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
    // don't add if we hit maxsize and are worse
    if (this.size() >= maxsize) {
      if (super.compareExternal(e, 0) < 0) {
        // while we did not change, this still was "successful".
        return true;
      }
    }
    boolean result = super.offer(e);
    // purge unneeded entrie(s)
    while (this.size() > maxsize) {
      // FIXME: only pop when there is a tie.
      poll();
    }
    return result;
  }
}
