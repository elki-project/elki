package experimentalcode.erich.utilities;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Basic in-memory heap structure. Closely related to a {@link PriorityQueue},
 * but here we can override methods to obtain e.g. a {@link TopBoundedHeap}
 * 
 * @author Erich Schubert
 * 
 * @param <E> Element type. Should be {@link Comparable} or a {@link Comparator} needs to be given.
 */
public class Heap<E> extends AbstractQueue<E> {
  // TODO: make serializable?
  
  /**
   * Heap storage
   */
  private transient Object[] queue;

  /**
   * Current number of objects
   */
  protected int size = 0;

  /**
   * The comparator or {@code null}
   */
  private final Comparator<? super E> comparator;

  /**
   * (Structural) modification counter. Used to invalidate iterators.
   */
  public transient int modCount = 0;

  /**
   * Default initial capacity
   */
  private static final int DEFAULT_INITIAL_CAPACITY = 11;

  /**
   * Default constructor: default capacity, natural ordering.
   */
  public Heap() {
    this(DEFAULT_INITIAL_CAPACITY, null);
  }

  /**
   * Constructor with initial capacity, natural ordering.
   * 
   * @param size initial size
   */
  public Heap(int size) {
    this(size, null);
  }

  /**
   * Constructor with {@link Comparator}.
   * @param comparator Comparator
   */
  public Heap(Comparator<? super E> comparator) {
    this(DEFAULT_INITIAL_CAPACITY, comparator);
  }

  /**
   * Constructor with initial capacity and {@link Comparator}.
   * @param size initial capacity
   * @param comparator Comparator
   */
  public Heap(int size, Comparator<? super E> comparator) {
    super();
    this.size = 0;
    this.queue = new Object[size];
    this.comparator = comparator;
  }

  @Override
  public synchronized boolean offer(E e) {
    // resize when needed
    considerResize(size + 1);
    final int parent = parent(size);
    // append element
    modCount++;
    queue[size] = e;
    this.size = size + 1;
    heapifyUp(parent);
    // We have changed - return true according to {@link Collection#put}
    return true;
  }

  @Override
  public synchronized E peek() {
    if(size == 0) {
      return null;
    }
    return castQueueElement(0);
  }

  @Override
  public synchronized E poll() {
    if(size == 0) {
      return null;
    }
    E ret = castQueueElement(0);
    removeAt(0);
    return ret;
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   */
  protected synchronized void removeAt(int pos) {
    modCount++;
    // remove!
    size = size - 1;
    queue[pos] = queue[size];
    // avoid dangling references!
    queue[size] = null;
    heapifyDown(pos);
  }

  /**
   * Compute parent index in heap array.
   * 
   * @param pos Element index
   * @return Parent index
   */
  private int parent(int pos) {
    return (pos - 1) / 2;
  }

  /**
   * Compute left child index in heap array.
   * 
   * @param pos Element index
   * @return left child index
   */
  private int leftChild(int pos) {
    return 2 * pos + 1;
  }

  /**
   * Compute right child index in heap array.
   * 
   * @param pos Element index
   * @return right child index
   */
  private int rightChild(int pos) {
    return 2 * pos + 2;
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   */
  private void heapifyUp(int pos) {
    if(pos < 0 || pos >= size) {
      return;
    }
    // precondition: both child trees are already sorted.
    final int parent = parent(pos);
    final int lchild = leftChild(pos);
    final int rchild = rightChild(pos);

    int min = pos;
    if(lchild < size) {
      if(compare(min, lchild) > 0) {
        min = lchild;
      }
    }
    if(rchild < size) {
      if(compare(min, rchild) > 0) {
        min = rchild;
      }
    }
    if(min != pos) {
      // swap with minimal element
      Object tmp = queue[pos];
      queue[pos] = queue[min];
      queue[min] = tmp;
      modCount++;
      heapifyUp(parent);
    }
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param pos re-insertion position
   */
  private void heapifyDown(int pos) {
    if(pos < 0 || pos >= size) {
      return;
    }
    final int lchild = leftChild(pos);
    final int rchild = rightChild(pos);

    int min = pos;
    if(lchild < size) {
      if(compare(min, lchild) > 0) {
        min = lchild;
      }
    }
    if(rchild < size) {
      if(compare(min, rchild) > 0) {
        min = rchild;
      }
    }
    if(min != pos) {
      // swap with minimal element
      Object tmp = queue[pos];
      queue[pos] = queue[min];
      queue[min] = tmp;
      modCount++;
      // recruse down
      heapifyDown(min);
    }
  }

  @SuppressWarnings("unchecked")
  protected int compare(int pos1, int pos2) {
    if(comparator != null) {
      return comparator.compare(castQueueElement(pos1), castQueueElement(pos2));
    }
    try {
      Comparable<E> c = (Comparable<E>) castQueueElement(pos1);
      return c.compareTo(castQueueElement(pos2));
    }
    catch(ClassCastException e) {
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  protected int compareExternal(E o1, int pos2) {
    if(comparator != null) {
      return comparator.compare(o1, castQueueElement(pos2));
    }
    try {
      Comparable<E> c = (Comparable<E>) o1;
      return c.compareTo(castQueueElement(pos2));
    }
    catch(ClassCastException e) {
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  protected E castQueueElement(int n) {
    return (E) queue[n];
  }

  @Override
  public int size() {
    return this.size;
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  private void considerResize(int requiredSize) {
    if(requiredSize > queue.length) {
      // Double until 64, then increase by 50% each time.
      int newCapacity = ((queue.length < 64) ? ((queue.length + 1) * 2) : ((queue.length / 2) * 3));
      // overflow?
      if(newCapacity < 0) {
        newCapacity = Integer.MAX_VALUE;
      }
      if(requiredSize > newCapacity) {
        newCapacity = requiredSize;
      }
      grow(newCapacity);
    }
  }

  /**
   * Execute the actual resize operation.
   * 
   * @param newsize New size
   */
  private void grow(int newsize) {
    // check for overflows
    if(newsize < 0) {
      throw new OutOfMemoryError();
    }
    if(newsize == queue.length) {
      return;
    }
    modCount++;
    queue = Arrays.copyOf(queue, newsize);
  }

  @Override
  public void clear() {
    modCount++;
    // clean up references in the array for memory management
    for(int i = 0; i < size; i++) {
      queue[i] = null;
    }
    this.size = 0;
  }

  @Override
  public boolean contains(Object o) {
    if(o != null) {
      for(int i = 0; i < size; i++) {
        if(o.equals(queue[i])) {
          return true;
        }
      }
    }
    return false;
  }

  // TODO: bulk add implementation of addAll?

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  /**
   * Iterator over queue elements. No particular order (i.e. heap order!)
   * 
   * @author Erich Schubert
   *
   */
  protected final class Itr implements Iterator<E> {
    /**
     * Cursor position
     */
    private int cursor = 0;

    /**
     * Modification counter this iterator is valid for.
     */
    private int expectedModCount = modCount;

    @Override
    public boolean hasNext() {
      return cursor < size;
    }

    @Override
    public E next() {
      if(expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
      if(cursor < size) {
        return castQueueElement(cursor++);
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      if(expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
      if(cursor > 0) {
        cursor--;
      }
      else {
        throw new IllegalStateException();
      }
      expectedModCount = modCount;
    }
  }
}