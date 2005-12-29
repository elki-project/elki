package de.lmu.ifi.dbs.utilities.heap.arrayheap;

import de.lmu.ifi.dbs.persistent.Page;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.heap.Identifiable;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.List;
import java.util.ArrayList;

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
  private List<HeapNode<K,V>> nodes;

  /**
   * The maximum size of the nodes array.
   */
  private int maxSize;

  /**
   * Construct a new empty slot page.
   */
  public SlotPage(int maxSize) {
    this.maxSize = maxSize;
    this.nodes = new ArrayList<HeapNode<K,V>>(maxSize);
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
    throw new UnsupportedOperationException();
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
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if this page is empty, false otherwise.
   * @return true if this page is empty, false otherwise
   */
  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  /**
   * Inserts the specified node into this page.
   * @param node the node to be inserted
   */
  public void insertNode(HeapNode<K,V> node) {
    nodes.add(node);
  }

  /**
   * Inserts the specified nodes into this page.
   * @param nodes the nodes to be inserted
   */
  public void insertAll(List<HeapNode<K,V>> nodes) {
    this.nodes.addAll(nodes);
  }

  /**
   * Returns the nodes of this page.
   * @return  the nodes of this page
   */
  public List<HeapNode<K,V>> getNodes() {
    return nodes;
  }

  /**
   * Clears the nodes of this page.
   */
  public void clearNodes() {
    nodes.clear();
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    if (id != null)
    return id.toString();
    return "tmp";
  }

}
