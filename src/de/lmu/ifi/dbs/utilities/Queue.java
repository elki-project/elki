package de.lmu.ifi.dbs.utilities;

import java.util.NoSuchElementException;

/**
 * Provides a simple queue with a linked list data structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Queue<O> {
  /**
   * The first of this queue, null if the queue is empty.
   */
  private QueueElement first;

  /**
   * The last element of this queue.
   */
  private QueueElement last;

  /**
   * Appends the specified object to the end of the queue.
   *
   * @param object the object to be enqueued
   */
  public void enqueue(O object) {
    if (first == null) {
      first = last = new QueueElement(object, null);
    }
    else {
      last.next = new QueueElement(object, null);
      last = last.next;
    }
  }

  /**
   * Removes the first object from the queue and returns it.
   *
   * @return the first object of the queue
   * @throws NoSuchElementException if the queue is empty
   */
  public O dequeue() {
    if (first == null) {
      throw new NoSuchElementException("No more elements");
    }

    O retval = first.object;
    QueueElement oldHead = first;
    first = first.next;
    if (first == null) {
      last = null;
    }
    else {
      oldHead.next = null;
    }
    return retval;
  }

  /**
   * Returns the first object of the queue, but does not remove it.
   *
   * @return the first object of the queue
   */
  public O firstObject() {
    if (first == null) {
      throw new NoSuchElementException("No more children");
    }

    return first.object;
  }

  /**
   * Returns true, if this queue is empty, false otherwise.
   *
   * @return true, if this queue is empty.
   */
  public boolean isEmpty() {
    return first == null;
  }

  /**
   * An element in the queue: holds the underlying objcet and the next element in the queue.
   */
  final class QueueElement {
    /**
     * Holds the underlying object.
     */
    private O object;

    /**
     * Points to the next element in the queue, null if end of queue is reached.
     */
    private QueueElement next;

    /**
     * Provides a new element in the queue.
     *
     * @param object the underlying object
     * @param next   the next element in the queue
     */
    private QueueElement(O object, QueueElement next) {
      this.object = object;
      this.next = next;
    }
  }

}
