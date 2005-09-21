package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.persistent.PageFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Default class for a node in a DeliRTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluNode extends AbstractNode {
  /**
   * Indicates wether the entry is already handled by the DeliClu algorithm or not.
   */
  private boolean handled[];

  /**
   * Empty constructor for Externalizable interface.
   */
  public DeLiCluNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public DeLiCluNode(PageFile<AbstractNode> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
    this.handled = new boolean[capacity];
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected AbstractNode createNewLeafNode(int capacity) {
    return new DeLiCluNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected AbstractNode createNewDirectoryNode(int capacity) {
    return new DeLiCluNode(file, capacity, false);
  }

  /**
   * Returns true, if the entry at the specified index is marked as handled, false otherwise.
   *
   * @param i the index of the entry
   * @return true, if the entry at the specified index is marked as handled, false otherwise
   */
  public final boolean isHandled(int i) {
    return handled[i];
  }

  /**
   * Marks the entry at the specified index as handled.
   *
   * @param i the index of the entry to be marked as handled
   */
  public void setHandled(int i) {
    handled[i] = true;
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
    super.writeExternal(out);
    out.writeObject(handled);
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
    super.readExternal(in);
    this.handled = (boolean []) in.readObject();
  }


}
