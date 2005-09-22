package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.persistent.PageFile;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A breadth first enumeration over the entries of a spatial index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BreadthFirstEnumeration<T extends SpatialNode> implements Enumeration<Entry> {
  /**
   * Represents an empty enumeration.
   */
  static public final Enumeration<Entry> EMPTY_ENUMERATION
  = new Enumeration<Entry>() {
    public boolean hasMoreElements() {
      return false;
    }

    public Entry nextElement() {
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
  PageFile<T> file;

  /**
   * Creates a new breadth first enumeration with the specified node as root node.
   *
   * @param rootEntry the root entry of the enumeration
   * @param file The file storing the nodes
   */
  public BreadthFirstEnumeration(final PageFile<T> file, final Entry rootEntry) {
    super();
    this.queue = new Queue();
    this.file = file;

    Enumeration<Entry> root_enum = new Enumeration<Entry>() {
      boolean hasNext = true;

      public boolean hasMoreElements() {
        return hasNext;
      }

      public Entry nextElement() {
        hasNext = false;
        return rootEntry;
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
   * @throws NoSuchElementException if no more elements exist.
   */
  public Entry nextElement() {
    Enumeration<Entry> enumer = queue.firstObject();
    Entry next = enumer.nextElement();

    Enumeration<Entry> children;
    if (next instanceof DirectoryEntry) {
      SpatialNode node = file.readPage(next.getID());
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
      public Enumeration<Entry> enumeration;
      public QNode next;	// null if end

      public QNode(Enumeration<Entry> enumeration, QNode next) {
        this.enumeration = enumeration;
        this.next = next;
      }
    }

    public void enqueue(Enumeration<Entry> entry) {
      if (head == null) {
        head = tail = new QNode(entry, null);
      }
      else {
        tail.next = new QNode(entry, null);
        tail = tail.next;
      }
    }

    public Enumeration<Entry> dequeue() {
      if (head == null) {
        throw new NoSuchElementException("No more children");
      }

      Enumeration<Entry> retval = head.enumeration;
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

    public Enumeration<Entry> firstObject() {
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
