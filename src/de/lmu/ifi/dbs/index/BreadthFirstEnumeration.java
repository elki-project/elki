package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.persistent.PageFile;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A breadth first enumeration over the entries of an index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BreadthFirstEnumeration<N extends Node> implements Enumeration<Identifier> {

  /**
   * Represents an empty enumeration.
   */
  static public final Enumeration<Identifier> EMPTY_ENUMERATION
  = new Enumeration<Identifier>() {
    public boolean hasMoreElements() {
      return false;
    }

    public Identifier nextElement() {
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
   * Creates a new breadth first enumeration with the specified node as root node.
   *
   * @param rootID the root entry of the enumeration
   * @param file   The file storing the nodes
   */
  public BreadthFirstEnumeration(final PageFile<N> file, final Identifier rootID) {
    super();
    this.queue = new Queue();
    this.file = file;

    Enumeration<Identifier> root_enum = new Enumeration<Identifier>() {
      boolean hasNext = true;

      public boolean hasMoreElements() {
        return hasNext;
      }

      public Identifier nextElement() {
        hasNext = false;
        return rootID;
      }
    };

    queue.enqueue(root_enum);
  }

  /**
   * Tests if this enumeration contains more elements.
   *
   * @return <code>true</code> if and only if this enumeration object
   *         contains at least one more element to provide;
   *         <code>false</code> otherwise.
   */
  public boolean hasMoreElements() {
    return (! queue.isEmpty() &&
            (queue.firstObject()).hasMoreElements());
  }

  /**
   * Returns the next element of this enumeration if this enumeration
   * object has at least one more element to provide.
   *
   * @return the next element of this enumeration.
   * @throws java.util.NoSuchElementException
   *          if no more elements exist.
   */
  public Identifier nextElement() {
    Enumeration<Identifier> enumeration = queue.firstObject();
    Identifier nextID = enumeration.nextElement();

    Enumeration<Identifier> children;
    if (nextID.isNodeID()) {
      N node = file.readPage(nextID.value());
      children = node.children();
    }
    else {
      children = EMPTY_ENUMERATION;
    }

    if (!enumeration.hasMoreElements()) {
      queue.dequeue();
    }
    if (children.hasMoreElements()) {
      queue.enqueue(children);
    }
    return nextID;
  }

  // A simple queue with a linked list data structure.
  class Queue {
    QNode head;  // null if empty
    QNode tail;

    final class QNode {
      public Enumeration<Identifier> enumeration;
      public QNode next;  // null if end

      public QNode(Enumeration<Identifier> enumeration, QNode next) {
        this.enumeration = enumeration;
        this.next = next;
      }
    }

    public void enqueue(Enumeration<Identifier> entry) {
      if (head == null) {
        head = tail = new QNode(entry, null);
      }
      else {
        tail.next = new QNode(entry, null);
        tail = tail.next;
      }
    }

    public Enumeration<Identifier> dequeue() {
      if (head == null) {
        throw new NoSuchElementException("No more children");
      }

      Enumeration<Identifier> retval = head.enumeration;
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

    public Enumeration<Identifier> firstObject() {
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
