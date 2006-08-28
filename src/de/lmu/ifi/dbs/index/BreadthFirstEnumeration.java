package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.data.DatabaseObject;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Provides a breadth first enumeration over the nodes of an index structure.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BreadthFirstEnumeration<O extends DatabaseObject, N extends Node<E>, E extends Entry> implements Enumeration<IndexPath> {

  /**
   * Represents an empty enumeration.
   */
  public final Enumeration<IndexPath<E>> EMPTY_ENUMERATION = new Enumeration<IndexPath<E>>() {
    public boolean hasMoreElements() {
      return false;
    }

    public IndexPath<E> nextElement() {
      throw new NoSuchElementException("No more children");
    }
  };

  /**
   * The queue for the enumeration.
   */
  private Queue queue;

  /**
   * The index storing the nodes.
   */
  private Index<O, N, E> index;

  /**
   * Creates a new breadth first enumeration with the specified node as root
   * node.
   *
   * @param rootPath the root entry of the enumeration
   * @param index    the index storing the nodes
   */
  public BreadthFirstEnumeration(final Index<O, N, E> index,
                                 final IndexPath<E> rootPath) {
    super();
    this.queue = new Queue();
    this.index = index;

    Enumeration<IndexPath<E>> root_enum = new Enumeration<IndexPath<E>>() {
      boolean hasNext = true;

      public boolean hasMoreElements() {
        return hasNext;
      }

      public IndexPath<E> nextElement() {
        hasNext = false;
        return rootPath;
      }
    };

    queue.enqueue(root_enum);
  }

  /**
   * Tests if this enumeration contains more elements.
   *
   * @return <code>true</code> if and only if this enumeration object
   *         contains at least one more element to provide; <code>false</code>
   *         otherwise.
   */
  public boolean hasMoreElements() {
    return (!queue.isEmpty() && (queue.firstObject()).hasMoreElements());
  }

  /**
   * Returns the next element of this enumeration if this enumeration object
   * has at least one more element to provide.
   *
   * @return the next element of this enumeration.
   * @throws java.util.NoSuchElementException
   *          if no more elements exist.
   */
  public IndexPath<E> nextElement() {
    Enumeration<IndexPath<E>> enumeration = queue.firstObject();
    IndexPath<E> nextPath = enumeration.nextElement();

    Enumeration<IndexPath<E>> children;
    if (nextPath.getLastPathComponent().getEntry().isLeafEntry()) {
      children = EMPTY_ENUMERATION;
    }
    else {
      N node = index.getNode(nextPath.getLastPathComponent().getEntry().getID());
      children = node.children(nextPath);
    }

    if (!enumeration.hasMoreElements()) {
      queue.dequeue();
    }
    if (children.hasMoreElements()) {
      queue.enqueue(children);
    }
    return nextPath;
  }

  // A simple queue with a linked list data structure.
  class Queue {
    QNode head; // null if empty

    QNode tail;

    final class QNode {
      public Enumeration<IndexPath<E>> enumeration;

      public QNode next; // null if end

      public QNode(Enumeration<IndexPath<E>> enumeration, QNode next) {
        this.enumeration = enumeration;
        this.next = next;
      }
    }

    public void enqueue(Enumeration<IndexPath<E>> entry) {
      if (head == null) {
        head = tail = new QNode(entry, null);
      }
      else {
        tail.next = new QNode(entry, null);
        tail = tail.next;
      }
    }

    public Enumeration<IndexPath<E>> dequeue() {
      if (head == null) {
        throw new NoSuchElementException("No more children");
      }

      Enumeration<IndexPath<E>> retval = head.enumeration;
      QNode oldHead = head;
      head = head.next;
      if (head == null) {
        tail = null;
      }
      else {
        oldHead.next = null;
      }
      return retval;
    }

    public Enumeration<IndexPath<E>> firstObject() {
      if (head == null) {
        throw new NoSuchElementException("No more children");
      }

      return head.enumeration;
    }

    public boolean isEmpty() {
      return head == null;
    }

  } // End of class Queue
}
