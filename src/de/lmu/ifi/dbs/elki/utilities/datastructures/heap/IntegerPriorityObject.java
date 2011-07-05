package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

/**
 * Object for a priority queue with integer priority. Can be used in the
 * {@link de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap UpdatableHeap}, since hashcode and equality use the stored objects
 * only, not the priority.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Stored object type.
 */
public class IntegerPriorityObject<O> implements PairInterface<Integer, O>, Comparable<IntegerPriorityObject<?>> {
  /**
   * Priority.
   */
  int priority;

  /**
   * Stored object. Private; since changing this will break an
   * {@link de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap UpdatableHeap}s Hash Map!
   */
  private O object;

  /**
   * Constructor.
   *
   * @param priority Priority
   * @param object Payload
   */
  public IntegerPriorityObject(int priority, O object) {
    super();
    this.priority = priority;
    this.object = object;
  }

  /**
   * Get the priority.
   * 
   * @return Priority
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Get the stored object payload
   * 
   * @return object data
   */
  public O getObject() {
    return object;
  }

  @Override
  public Integer getFirst() {
    return priority;
  }

  @Override
  public O getSecond() {
    return object;
  }

  @Override
  public int hashCode() {
    return ((object == null) ? 0 : object.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof IntegerPriorityObject)) {
      return false;
    }
    IntegerPriorityObject<?> other = (IntegerPriorityObject<?>) obj;
    if(object == null) {
      return (other.object == null);
    }
    else {
      return object.equals(other.object);
    }
  }

  @Override
  public int compareTo(IntegerPriorityObject<?> o) {
    return o.priority - this.priority;
  }
}