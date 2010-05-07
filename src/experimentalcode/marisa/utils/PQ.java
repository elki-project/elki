package experimentalcode.marisa.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;

/**
 * Implements a priority queue for objects.
 * 
 * @author Matthias Schubert(creation: 2002-5-11)
 * @version 1.0
 */
public class PQ<D extends Distance<D>, T> implements Serializable {

  private static final long serialVersionUID = 9220492383597206913L;

  /**
   * This pair type is used to store the objects together with their priority in
   * one structure.
   */
  public class Pair {

    D prio;

    T obj;

    public Pair(D priority, T object) {
      prio = priority;
      obj = object;
    }

    public D getPriority() {
      return prio;
    }

    public T getValue() {
      return obj;
    }
  }

  /**
   * Symbolic constant indicating ascending sorting order.
   * 
   * @see #Descending
   */
  public static final boolean Ascending = true;

  /**
   * Symbolic constant indicating descending sorting order.
   * 
   * @see #Ascending
   */
  public static final boolean Descending = false;

  /** The list of objects. */
  protected Pair[] queue;

  /**
   * Indicates the sorting order. The queue is sorted in ascending order if asc
   * is true and with descending priority otherwise.
   */
  protected boolean asc;

  protected int lastIndex;

  /**
   * Performs a copy of <code>this</code>, using references to the elements in
   * {@link #queue}.
   * 
   * @return copy of this queue
   */
  public PQ<D, T> copy() {
    PQ<D, T> pq = new PQ<D, T>(queue.length);
    System.arraycopy(queue, 0, pq.queue, 0, queue.length);
    pq.lastIndex = this.lastIndex;
    pq.asc = this.asc;
    return pq;
  }

  /**
   * Standard constructor of the PriorityQueue class. It creates a priority
   * queue which is sorted with ascending priority.
   */
  public PQ() {
    // size grows automatically!
    this(true, 100);
  }

  /**
   * Standard constructor of the PriorityQueue class. It creates a priority
   * queue which is sorted with ascending priority.
   * @param k Initial capacity of the queue
   */
  public PQ(int maxSize) {
    this(true, maxSize);
  }

  /**
   * Creates a PriorityQueue with the given sorting order.
   * 
   * @param ascending If this parameter is set 'true' the sorting order is
   *        ascending, otherwise descending.
   * @since 1.1
   */
  public PQ(boolean ascending) {
    // size grows automatically!
    this(ascending, 100);
  }

  /**
   * Creates a PriorityQueue with the given sorting order.
   * 
   * @param ascending If this parameter is set 'true' the sorting order is
   *        ascending, otherwise descending.
   * @param k Initial capacity of the queue
   * @since 1.1
   */
  public PQ(boolean ascending, int k) {
    queue = (Pair[]) new PQ<?, ?>.Pair[k];
    lastIndex = 0;
    asc = ascending;
  }

  /**
   * Adds an object and ensures that maxSize of the queue is not exceeded. This
   * means, that the element is only added, if its priority is better than the
   * worst one.
   * 
   * @param priority The priority/weight of the object.
   * @param obj The object that is added.
   * @param maxSize maximum size of the queue
   */
  public void addSecure(D priority, T obj, int maxSize) {
    if(size() >= maxSize) {
      if((asc && priority.compareTo(firstPriority()) > 0) || (!asc && priority.compareTo(firstPriority()) < 0)) {
        removeFirst();
        add(priority, obj);
      }
    }
    else {
      add(priority, obj);
    }
  }

  /**
   * @see #add(double, java.lang.Object)
   * @param p
   */
  public void add(PQ<D, T>.Pair p) {
    queue[lastIndex] = p;
    lastIndex++;
    ensureCapacity(lastIndex + 1);
    if(asc) {
      sift_up();
    }
    else {
      sift_up_rev();
    }
  }

  /**
   * adds another PriorityQueue to this queue by removing all of other's
   * elements.
   * 
   * @param other
   */
  public void add(PQ<D, T> other) {
    while(!other.isEmpty()) {
      add(other.removeFirstEntry());
    }
  }

  /**
   * copied from ArrayList to automatically increase the array size
   */
  private void ensureCapacity(int minCapacity) {
    int oldCapacity = queue.length;
    if(minCapacity > oldCapacity) {
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      if(newCapacity < minCapacity) {
        newCapacity = minCapacity;
      }
      // minCapacity is usually close to size, so this is a win:
      queue = Arrays.copyOf(queue, newCapacity);
    }
  }

  /**
   * Adds an object to the queue at the appropriate position. If the size of the
   * queue is exceeded, it will be increased automatically.
   * 
   * @param priority The priority of the object.
   * @param obj The object that is added.
   * @see #ensureCapacity(int)
   */
  public void add(D priority, T obj) {
    add(new Pair(priority, obj));
  }

  protected D val(int x) {
    return queue[x].prio;
  }

  /**
   * Lowers a key from the top
   */
  protected void sift_up() {
    int akt = lastIndex;
    int comp = akt / 2;
    Pair x = queue[akt - 1];
    while(comp > 0 && val(comp - 1).compareTo(x.prio) > 0) {
      queue[akt - 1] = queue[comp - 1];
      akt = comp;
      comp = akt / 2;
    }
    queue[akt - 1] = x;
  }

  /**
   * Lowers a key from the top
   */
  protected void sift_down() {
    int akt = 1;
    int comp = 2 * akt;
    Pair x = queue[akt - 1];
    if(comp < lastIndex && (val(comp).compareTo(val(comp - 1)) < 0)) {
      comp++;
    }
    while(comp <= lastIndex && val(comp - 1).compareTo(x.prio) < 0) {
      queue[akt - 1] = queue[comp - 1];
      akt = comp;
      comp = 2 * akt;
      if(comp < lastIndex && (val(comp).compareTo(val(comp - 1)) < 0)) {
        comp++;
      }
    }
    queue[akt - 1] = x;
  }

  /**
   * Lowers a key from the top
   */
  protected void sift_up_rev() {
    int akt = lastIndex;
    int comp = akt / 2;
    Pair x = queue[akt - 1];
    while(comp > 0 && val(comp - 1).compareTo(x.prio) < 0) {
      queue[akt - 1] = queue[comp - 1];
      akt = comp;
      comp = akt / 2;
    }
    queue[akt - 1] = x;
  }

  /**
   * Lowers a key from the top
   */
  protected void sift_down_rev() {
    int akt = 1;
    int comp = 2 * akt;
    Pair x = queue[akt - 1];
    if((comp < lastIndex) && (val(comp).compareTo(val(comp - 1)) > 0)) {
      comp++;
    }
    while(comp <= lastIndex && val(comp - 1).compareTo(x.prio) > 0) {
      queue[akt - 1] = queue[comp - 1];
      akt = comp;
      comp = 2 * akt;
      if((comp < lastIndex) && (val(comp).compareTo(val(comp - 1)) > 0)) {
        comp++;
      }
    }
    queue[akt - 1] = x;
  }

  /**
   * Returns the size of the priority queue.
   * 
   * @return the number of elements in the priority queue.
   */
  public int size() {
    return lastIndex;
  }

  /**
   * Returns the priority of the first object in the queue.
   */
  public D firstPriority() {
    return queue[0].prio;
  }

  /**
   * Resets this PQ s.t. no more elements are officially contained. This
   * operation, however, does <em>not</em> reset the array structure containing
   * the values, the array thus keeps all previously needed elements in memory.
   */
  public void init() {
    lastIndex = 0;
  }

  public boolean isEmpty() {
    return lastIndex == 0;
  }

  /**
   * Indicates if the sorting order is ascending or descending.
   * 
   * @return 'true' if the sorting order for the priority queue is ascending,
   *         'false if the sorting order is descending.
   * @since 1.1
   */
  public final boolean isAscending() {
    return asc;
  }

  public Pair getFirstEntry() {
    return queue[0];
  }

  public T getFirst() {
    return queue[0].obj;
  }

  public Pair removeFirstEntry() {
    Pair p = getFirstEntry();
    removeFirst();
    return p;
  }

  /**
   * Removes the first object from the queue and returns that object to the
   * caller.
   * 
   * @return the first object in the queue.
   */
  public T removeFirst() {
    T obj = queue[0].obj;
    lastIndex--;
    queue[0] = queue[lastIndex];
    if(asc) {
      sift_down();
    }
    else {
      sift_down_rev();
    }
    return obj;
  }

  public int getCapacity() {
    return queue.length;
  }

  /**
   * Doubles the capacity of <code>this</code>.
   */
  public void doubleQueue() {
    Pair[] newQueue = (Pair[]) new PQ<?, ?>.Pair[queue.length * 2];
    newQueue = Arrays.copyOf(queue, queue.length * 2, newQueue.getClass());
    queue = newQueue;
  }

  public static void main(String[] args) {
    PQ<IntegerDistance, String> q = new PQ<IntegerDistance, String>(15);
    q.addSecure(new IntegerDistance(1), " 1", 15);
    q.addSecure(new IntegerDistance(1), " 1", 15);
    q.addSecure(new IntegerDistance(1), " 1", 15);
    q.addSecure(new IntegerDistance(1), " 1", 15);
    q.addSecure(new IntegerDistance(7), " 17", 15);
    q.addSecure(new IntegerDistance(0), " 0", 15);
    q.addSecure(new IntegerDistance(134), " 134", 15);
    q.addSecure(new IntegerDistance(123), " 123", 15);
    q.addSecure(new IntegerDistance(13), " 13", 15);
    q.addSecure(new IntegerDistance(11), " 11", 15);
    q.addSecure(new IntegerDistance(11), " 11", 15);
    q.addSecure(new IntegerDistance(17), " 17", 15);
    q.addSecure(new IntegerDistance(14), " 14", 15);
    q.addSecure(new IntegerDistance(114), " 114", 15);
    q.addSecure(new IntegerDistance(0), " 0", 15);
    q.addSecure(new IntegerDistance(0), " 0", 15);

    PQ<IntegerDistance, String> copyQ = q.copy();

    while(!q.isEmpty()) {
      System.out.println(q.removeFirst());
    }

    System.out.println("copy:");

    while(!copyQ.isEmpty()) {
      System.out.println(copyQ.removeFirst());
    }

  }

  public List<T> asList() {
    PQ<D, T> pq = copy();
    List<T> list = new ArrayList<T>(size());
    while(!pq.isEmpty()) {
      list.add(pq.removeFirst());
    }
    return list;
  }
}
