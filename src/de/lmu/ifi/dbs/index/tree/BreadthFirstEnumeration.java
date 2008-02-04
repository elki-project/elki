package de.lmu.ifi.dbs.index.tree;

import de.lmu.ifi.dbs.data.DatabaseObject;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Provides a breadth first enumeration over the nodes of an index structure.
 *
 * @author Elke Achtert 
 */
public class BreadthFirstEnumeration<O extends DatabaseObject, N extends Node<N,E>, E extends Entry> implements Enumeration<TreeIndexPath<E>> {

  /**
   * Represents an empty enumeration.
   */
  public final Enumeration<TreeIndexPath<E>> EMPTY_ENUMERATION = new Enumeration<TreeIndexPath<E>>() {
    public boolean hasMoreElements() {
      return false;
    }

    public TreeIndexPath<E> nextElement() {
      throw new NoSuchElementException("No more children");
    }
  };

  /**
   * The queue for the enumeration.
   */
  private Queue<Enumeration<TreeIndexPath<E>>> queue;

  /**
   * The index storing the nodes.
   */
  private TreeIndex<O, N, E> index;

  /**
   * Creates a new breadth first enumeration with the specified node as root
   * node.
   *
   * @param rootPath the root entry of the enumeration
   * @param index    the index storing the nodes
   */
  public BreadthFirstEnumeration(final TreeIndex<O, N, E> index,
                                 final TreeIndexPath<E> rootPath) {
    super();
    this.queue = new LinkedList<Enumeration<TreeIndexPath<E>>>();
    this.index = index;

    Enumeration<TreeIndexPath<E>> root_enum = new Enumeration<TreeIndexPath<E>>() {
      boolean hasNext = true;

      public boolean hasMoreElements() {
        return hasNext;
      }

      public TreeIndexPath<E> nextElement() {
        hasNext = false;
        return rootPath;
      }
    };

    queue.offer(root_enum);
  }

  /**
   * Tests if this enumeration contains more elements.
   *
   * @return <code>true</code> if and only if this enumeration object
   *         contains at least one more element to provide; <code>false</code>
   *         otherwise.
   */
  public boolean hasMoreElements() {
    return (!queue.isEmpty() && (queue.peek()).hasMoreElements());
  }

  /**
   * Returns the next element of this enumeration if this enumeration object
   * has at least one more element to provide.
   *
   * @return the next element of this enumeration.
   * @throws java.util.NoSuchElementException
   *          if no more elements exist.
   */
  public TreeIndexPath<E> nextElement() {
    Enumeration<TreeIndexPath<E>> enumeration = queue.peek();
    TreeIndexPath<E> nextPath = enumeration.nextElement();

    Enumeration<TreeIndexPath<E>> children;
    if (nextPath.getLastPathComponent().getEntry().isLeafEntry()) {
      children = EMPTY_ENUMERATION;
    }
    else {
      N node = index.getNode(nextPath.getLastPathComponent().getEntry().getID());
      children = node.children(nextPath);
    }

    if (!enumeration.hasMoreElements()) {
      queue.remove();
    }
    if (children.hasMoreElements()) {
      queue.offer(children);
    }
    return nextPath;
  }
}
