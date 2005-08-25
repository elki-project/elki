package de.lmu.ifi.dbs.index.spatial;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A breadth first enumeration over the entries of a spatial index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BreadthFirstEnumeration implements Enumeration<SpatialObject> {
  /**
   * Represents an empty enumeration.
   */
  static public final Enumeration<SpatialObject> EMPTY_ENUMERATION
  = new Enumeration<SpatialObject>() {
    public boolean hasMoreElements() {
      return false;
    }

    public SpatialObject nextElement() {
      throw new NoSuchElementException("No more children");
    }
  };

  /**
   * The queue for the enumeration.
   */
  private Queue queue;

  /**
   * Creates a new breadth first enumeration with the specified node as root node.
   *
   * @param rootNode the root node of the enumeration
   */
  public BreadthFirstEnumeration(final SpatialNode rootNode) {
    super();
    queue = new Queue();

    Enumeration<SpatialObject> root_enum
    = new Enumeration<SpatialObject>() {
      boolean hasNext = true;

      public boolean hasMoreElements() {
        return hasNext;
      }

      public SpatialObject nextElement() {
        hasNext = false;
        return rootNode;
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
    return (!queue.isEmpty() &&
            ((Enumeration) queue.firstObject()).hasMoreElements());
  }

  /**
   * Returns the next element of this enumeration if this enumeration
   * object has at least one more element to provide.
   *
   * @return the next element of this enumeration.
   * @throws NoSuchElementException if no more elements exist.
   */
  public SpatialObject nextElement() {
    Enumeration<SpatialObject> enumer = queue.firstObject();
    SpatialObject next = enumer.nextElement();

    Enumeration<SpatialObject> children;
    if (next instanceof SpatialNode) {
      SpatialNode node = (SpatialNode) next;
      children = node.children();
    }
    else {
      children = EMPTY_ENUMERATION;
    }

    if (!enumer.hasMoreElements()) {
      queue.dequeue();
    }
    if (children.hasMoreElements()) {
      queue.enqueue(children);
    }
    return next;
  }

  // A simple queue with a linked list data structure.
  final class Queue {
    QNode head;	// null if empty
    QNode tail;

    final class QNode {
      public Enumeration<SpatialObject> enumeration;
      public QNode next;	// null if end

      public QNode(Enumeration<SpatialObject> enumeration, QNode next) {
        this.enumeration = enumeration;
        this.next = next;
      }
    }

    public void enqueue(Enumeration<SpatialObject> entry) {
      if (head == null) {
        head = tail = new QNode(entry, null);
      }
      else {
        tail.next = new QNode(entry, null);
        tail = tail.next;
      }
    }

    public Enumeration<SpatialObject> dequeue() {
      if (head == null) {
        throw new NoSuchElementException("No more children");
      }

      Enumeration<SpatialObject> retval = head.enumeration;
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

    public Enumeration<SpatialObject> firstObject() {
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
