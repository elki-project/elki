package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.persistent.PageFile;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Provides a breadth first enumeration over the nodes of an index structure.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BreadthFirstEnumeration<N extends Node> implements
                                                     Enumeration<IndexPath> {

  /**
   * Represents an empty enumeration.
   */
  static public final Enumeration<IndexPath> EMPTY_ENUMERATION = new Enumeration<IndexPath>() {
    public boolean hasMoreElements() {
      return false;
    }

    public IndexPath nextElement() {
      throw new NoSuchElementException("No more children");
    }
  };

  /**
   * The queue for the enumeration.
   */
  private Queue queue;

  /**
   * The file storing the nodes.
   */
  PageFile<N> file;

  /**
   * Creates a new breadth first enumeration with the specified node as root
   * node.
   *
   * @param rootPath the root entry of the enumeration
   * @param file     The file storing the nodes
   */
  public BreadthFirstEnumeration(final PageFile<N> file,
                                 final IndexPath rootPath) {
    super();
    this.queue = new Queue();
    this.file = file;

    Enumeration<IndexPath> root_enum = new Enumeration<IndexPath>() {
      boolean hasNext = true;

      public boolean hasMoreElements() {
        return hasNext;
      }

      public IndexPath nextElement() {
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
  public IndexPath nextElement() {
    Enumeration<IndexPath> enumeration = queue.firstObject();
    IndexPath nextPath = enumeration.nextElement();

    Enumeration<IndexPath> children;
    if (nextPath.getLastPathComponent().getEntry().isLeafEntry()) {
      children = EMPTY_ENUMERATION;
    }
    else {
      N node = file.readPage(nextPath.getLastPathComponent().getEntry().getID());
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
      public Enumeration<IndexPath> enumeration;

      public QNode next; // null if end

      public QNode(Enumeration<IndexPath> enumeration, QNode next) {
        this.enumeration = enumeration;
        this.next = next;
      }
    }

    public void enqueue(Enumeration<IndexPath> entry) {
      if (head == null) {
        head = tail = new QNode(entry, null);
      }
      else {
        tail.next = new QNode(entry, null);
        tail = tail.next;
      }
    }

    public Enumeration<IndexPath> dequeue() {
      if (head == null) {
        throw new NoSuchElementException("No more children");
      }

      Enumeration<IndexPath> retval = head.enumeration;
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

    public Enumeration<IndexPath> firstObject() {
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
