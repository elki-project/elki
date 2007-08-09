package de.lmu.ifi.dbs.utilities;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Provides a breadth first enumeration over enumeratable objects.
 *
 * @author Elke Achtert 
 */
public class BreadthFirstEnumeration<E extends Enumeratable<E>> implements Enumeration<E> {

  /**
   * Represents an empty enumeration.
   */
  public final Enumeration<E> EMPTY_ENUMERATION = new Enumeration<E>() {
    public boolean hasMoreElements() {
      return false;
    }

    public E nextElement() {
      throw new NoSuchElementException("No more children");
    }
  };

  /**
   * The queue for the enumeration.
   */
  private Queue<E> queue;

  /**
   * Creates a new breadth first enumeration with the specified node as root
   * node.
   *
   * @param root the root of the enumeration
   */
  public BreadthFirstEnumeration(final E root) {
    super();
    queue = new LinkedList<E>();
    queue.offer(root);
  }

  /**
   * Tests if this enumeration contains more elements.
   *
   * @return <code>true</code> if and only if this enumeration object
   *         contains at least one more element to provide; <code>false</code>
   *         otherwise.
   */
  public boolean hasMoreElements() {
    return !queue.isEmpty();
  }

  /**
   * Returns the next element of this enumeration if this enumeration object
   * has at least one more element to provide.
   *
   * @return the next element of this enumeration.
   * @throws java.util.NoSuchElementException
   *          if no more elements exist.
   */
  public E nextElement() {
    E next = queue.remove();

    for (int i = 0; i < next.numChildren(); i++) {
      queue.offer(next.getChild(i));
    }

    return next;
  }
}
