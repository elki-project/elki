package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import java.util.Comparator;
import java.util.HashMap;

/**
 * A heap as used in OPTICS that allows updating entries.
 * 
 * @author Erich Schubert
 * 
 * @param <O> object type
 */
public class UpdatableHeap<O> extends Heap<O> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Holds the indices in the heap of each element.
   */
  private HashMap<O, Integer> index = new HashMap<O, Integer>();

  /**
   * Simple constructor with default size.
   */
  public UpdatableHeap() {
    super();
  }

  /**
   * Constructor with predefined size.
   * 
   * @param size Size
   */
  public UpdatableHeap(int size) {
    super(size);
  }

  /**
   * Constructor with comparator
   * 
   * @param comparator Comparator
   */
  public UpdatableHeap(Comparator<? super O> comparator) {
    super(comparator);
  }

  /**
   * Constructor with predefined size and comparator
   * 
   * @param size Size
   * @param comparator Comparator
   */
  public UpdatableHeap(int size, Comparator<? super O> comparator) {
    super(size, comparator);
  }

  @Override
  public void clear() {
    super.clear();
    index.clear();
  }

  @Override
  public synchronized boolean offer(O e) {
    Integer pos = index.get(e);
    if(pos == null) {
      // LoggingUtil.logExpensive(Level.INFO, "Inserting: "+e);
      // insert
      return super.offer(e);
    }
    else {
      // update
      if(compareExternal(e, pos) < 0) {
        // LoggingUtil.logExpensive(Level.INFO,
        // "Updating value: "+e+" vs. "+castQueueElement(pos));
        modCount++;
        putInQueue(pos, e);
        heapifyUpParent(pos);
        // We have changed - return true according to {@link Collection#put}
        return true;
      }
      else {
        // LoggingUtil.logExpensive(Level.INFO,
        // "Keeping value: "+e+" vs. "+castQueueElement(pos));
        // Ignore, no improvement. Return success anyway.
        return true;
      }
    }
  }

  @Override
  protected void putInQueue(int pos, Object e) {
    super.putInQueue(pos, e);
    // Keep index up to date
    if(e != null) {
      O n = castQueueElement(pos);
      index.put(n, pos);
    }
  }

  @Override
  protected synchronized O removeAt(int pos) {
    O node = super.removeAt(pos);
    // Keep index up to date
    index.remove(node);
    return node;
  }

  /**
   * Remove the given object from the queue.
   * 
   * @param e Obejct to remove
   * @return Existing entry
   */
  public O removeObject(O e) {
    Integer pos = index.get(e);
    if(pos != null) {
      return removeAt(pos);
    }
    else {
      return null;
    }
  }

  @Override
  public O poll() {
    O node = super.poll();
    index.remove(node);
    return node;
  }
}