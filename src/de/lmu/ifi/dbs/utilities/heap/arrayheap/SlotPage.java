package de.lmu.ifi.dbs.utilities.heap.arrayheap;

import de.lmu.ifi.dbs.persistent.Page;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.heap.Identifiable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class SlotPage<K extends Comparable<K>, V extends Identifiable> implements Page {

  /**
   * The unique id of this page.
   */
  private Integer id;

  /**
   * The dirty flag.
   */
  private boolean dirty;

  /**
   * The array of heap nodes belonging to this page.
   */
  private SortedSet<HeapNode<K, V>> nodes;

  /**
   * The maximum size of the nodes array.
   */
  private int maxSize;

  /**
   * Empty constructor for externalizable purposes.
   */
  public SlotPage() {
  }

  /**
   * Construct a new empty slot page.
   */
  public SlotPage(int maxSize) {
    this.maxSize = maxSize;
    this.nodes = new TreeSet<HeapNode<K, V>>();
  }

  /**
   * Returns the unique id of this Page.
   *
   * @return the unique id of this Page
   */
  public Integer getID() {
    return id;
  }

  /**
   * Sets the unique id of this Page.
   *
   * @param id the id to be set
   */
  public void setID(int id) {
    this.id = id;
  }

  /**
   * Sets the page file of this page.
   *
   * @param file the page file to be set
   */
  public void setFile(PageFile file) {
  }

  /**
   * Returns true if this page is dirty, false otherwise.
   *
   * @return true if this page is dirty, false otherwise
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Sets the dirty flag of this page.
   *
   * @param dirty the dirty flag to be set
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe
   * the data layout of this Externalizable object.
   * List the sequence of element types and, if possible,
   * relate the element to a public/protected field and/or
   * method of this Externalizable class.
   * todo
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   * todo
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if this page is empty, false otherwise.
   *
   * @return true if this page is empty, false otherwise
   */
  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  /**
   * Inserts the specified node into this page.
   *
   * @param node the node to be inserted
   */
  public void insertNode(HeapNode<K, V> node) {
    if (nodes.size() == maxSize)
      throw new IllegalStateException("Maximum size of page is reached!");
    nodes.add(node);
  }

  /**
   * Inserts the nodes of the specified page into this page. The nodes in the specified page
   * will be removed.
   *
   * @param otherPage the page containing the nodes to be inserted
   */
  public void insertNodesFrom(SlotPage<K, V> otherPage) {
    if (this.nodes.size() + nodes.size() > maxSize)
      throw new IllegalStateException("Maximum size of page is reached!");

    this.nodes.addAll(otherPage.nodes);
    otherPage.nodes.clear();
  }

  /**
   * Returns the first node of this page.
   * @return  the first node of this page if this page contains nodes, null otherwise.
   */
  public HeapNode<K, V> firstNode() {
    if (nodes.isEmpty()) return null;
    return nodes.first();
  }

  /**
   * Returns the last node of this page.
   * @return  the last node of this page if this page contains nodes, null otherwise.
   */
  public HeapNode<K, V> lastNode() {
    if (nodes.isEmpty()) return null;
    return nodes.last();
  }

  /**
   * Removes the first node from this page.
   */
  public HeapNode<K,V> removeFirstNode() {
    if (nodes.isEmpty())
      throw new IllegalStateException("This page is empty!");

    HeapNode<K,V> first = firstNode();
    nodes.remove(first);

    dirty = true;
    return first;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return id.toString();
  }

  /**
   * Returns a string representation of the nodes of this page.
   *
   * @return a string representation of the nodes of this page.
   */
  public String nodesToString() {
    return nodes.toString();
  }

}
