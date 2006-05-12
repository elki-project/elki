package de.lmu.ifi.dbs.index.btree;

import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.persistent.Page;
import de.lmu.ifi.dbs.persistent.PageFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * BTreeNode denotes a node in a BTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BTreeNode<K extends Comparable<K> & Serializable, V extends Serializable> implements Page {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The order of the BTree: determines the maximum number of entries (2*m)
   */
  int m;

  /**
   * The unique id if this node.
   */
  Integer nodeID;

  /**
   * The number of entries in this node (<= 2 * m).
   */
  int numKeys;

  /**
   * The data entries of this node.
   */
  public BTreeData<K, V>[] data;

  /**
   * The ids of the subtrees (children) of this node.
   */
  Integer childIDs[];

  /**
   * Indicates weather this node is a leaf or an inner node.
   */
  boolean isLeaf;

  /**
   * The id of the parent of this node.
   */
  Integer parentID;

  /**
   * The file that stores the nodes of the BTree.
   */
  PageFile<BTreeNode<K, V>> file;

  /**
   * The dirty flag of this page.
   */
  boolean dirty;

  /**
   * Creates a new BTreeNode.
   */
  public BTreeNode() {
  }

  /**
   * Creates a new BTreeNode with the specified parameters.
   *
   * @param m        the order of the BTree
   * @param parentID the ID of the parent of this new node
   */
  BTreeNode(int m, Integer parentID, PageFile<BTreeNode<K, V>> file) {
    this.m = m;
    this.parentID = parentID;
    this.file = file;

    // one more for overflow
    //noinspection unchecked
    this.data = (BTreeData<K, V>[]) new BTreeData<?, ?>[2 * m + 1];
    this.childIDs = new Integer[2 * m + 2];

    // default is true
    isLeaf = true;
  }

  /**
   * Inserts a data object into the right position. This method is only allowed in leaf nodes.
   *
   * @param data the data to be inserted
   */
  void insert(BTreeData<K, V> data) {
    if (!isLeaf)
      throw new IllegalStateException("Node must be a leaf!");

    // first entry
    if (numKeys == 0) {
      numKeys++;
      this.data[0] = data;
      file.writePage(this);
      return;
    }

    // search for the right index and insert the data
    int index = 0;
    while (data.key.compareTo(this.data[index].key) > 0) {
      index++;
      if (index == numKeys) break;
    }
    // key already exists: set value and return
    if ((index < numKeys) && data.key.compareTo(this.data[index].key) == 0) {
      this.data[index].value = data.value;
      file.writePage(this);
      return;
    }
    // insert
    else {
      shiftRight(index, data);
      file.writePage(this);
    }

    // overflow -> split this node
    if (numKeys > 2 * m) {
      overflowTreatment();
    }
  }

  /**
   * Deletes the data object at the specified index in this node. This method is only allowed in leaf nodes.
   *
   * @param index the index of the data to be deleted
   */
  BTreeData<K, V> delete(int index) {
    // delete in leaf
    if (isLeaf) {
      // delete the data entry
      BTreeData<K, V> delData = shiftLeft(index, true);
      file.writePage(this);

      // underflow
      if (numKeys < m) {
        underflowTreatment();
      }
      return delData;
    }

    // swap with smallest of the greater keys
    else {
      // get smallest of the greater keys
      BTreeNode<K, V> child = file.readPage(childIDs[index + 1]);
      while (!child.isLeaf) {
        child = file.readPage(child.childIDs[0]);
      }
      BTreeData<K, V> swappy = child.shiftLeft(0, true);
      BTreeData<K, V> delData = data[index];
      data[index] = swappy;
      file.writePage(this);
      file.writePage(child);

      if (child.numKeys < m) child.underflowTreatment();
      return delData;
    }
  }

  /**
   * Returns the unique id of this Page.
   *
   * @return the unique id of this Page
   */
  public Integer getID() {
    return nodeID;
  }

  /**
   * Sets the unique id of this Page.
   *
   * @param id the id to be set
   */
  public void setID(int id) {
    this.nodeID = id;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object
   */
  public String toString() {
    if (nodeID != null)
      return Integer.toString(nodeID);
    else
      return "null";
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
    out.writeInt(m);
    out.writeInt(nodeID);
    out.writeInt(numKeys);
    out.writeBoolean(isLeaf);
    if (parentID != null)
      out.writeInt(parentID);
    else
      out.writeInt(-1);

    for (BTreeData<K, V> d : data) {
      out.writeObject(d);
    }

    for (Integer childID : childIDs) {
      if (childID == null)
        out.writeInt(-1);
      else
        out.writeInt(childID);
    }
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
    m = in.readInt();
    nodeID = in.readInt();
    numKeys = in.readInt();
    isLeaf = in.readBoolean();
    parentID = in.readInt();
    if (parentID == -1) parentID = null;
    this.dirty = false;

    //noinspection unchecked
    this.data = (BTreeData<K, V>[]) new BTreeData<?, ?>[2 * m + 1];
    for (int i = 0; i < data.length; i++) {
      data[i] = (BTreeData<K, V>) in.readObject();
    }

    this.childIDs = new Integer[2 * m + 2];
    for (int i = 0; i < childIDs.length; i++) {
      int childID = in.readInt();
      if (childID != -1) childIDs[i] = childID;
      else childIDs[i] = null;
    }
  }

  /**
   * Sets the page file of this page.
   *
   * @param file the page file to be set
   */
  public void setFile(PageFile file) {
    //noinspection unchecked
    this.file = file;
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
   * Sets the dirty flag of this page
   *
   * @param dirty the dirty flag to be set
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * Treatment of an overflow in this node: splits this node into two nodes.
   * This can only be done, if this node is full.
   * The key in the middle is moved up into the parent, the left ones rest in this
   * node, and the right ones go into a new node. The split can be propagated up to
   * the root and then the height of the BTree increases .
   */
  private void overflowTreatment() {
    StringBuffer msg = new StringBuffer();

    if (numKeys < 2 * m)
      throw new IllegalStateException("This node has no overflow!");

    // special case: this node is the root
    if (parentID == null && this.nodeID == 0) {
      splitRoot();
      return;
    }

    BTreeNode<K, V> parent = file.readPage(parentID);
    // get the new index in parent
    int index = 0;
    while (data[m - 1].key.compareTo(parent.data[index].key) > 0) {
      index++;
      if (index == parent.numKeys) break;
    }
    if (DEBUG) {
      msg.append(index);
      msg.append("\n parent  bef ");
      msg.append(Arrays.asList(parent.data));
      msg.append(Arrays.asList(parent.childIDs));
    }

    // create the new node
    BTreeNode<K, V> newNode = new BTreeNode<K, V>(m, parentID, file);
    file.writePage(newNode);

    // shift the data in parent one position right and insert data[m]
    parent.shiftRight(index, data[m], newNode.nodeID, true);
    if (DEBUG) {
      msg.append("\n parent  aft ");
      msg.append(Arrays.asList(parent.data));
      msg.append(Arrays.asList(parent.childIDs));
    }

    // move the data entries
    for (int i = 0; i < 2 * m + 1; i++) {
      if (i < m) {
        newNode.data[i] = data[i];
        data[i] = data[m + i + 1];
      }
      else {
        data[i] = null;
      }
    }

    // move the children
    if (!isLeaf) {
      newNode.isLeaf = false;
      for (int i = 0; i < 2 * m + 2; i++) {
        if (i < m + 1) {
          BTreeNode<K, V> child = file.readPage(childIDs[i]);
          newNode.childIDs[i] = child.nodeID;
          child.parentID = newNode.nodeID;
          file.writePage(child);
          childIDs[i] = childIDs[m + i + 1];
        }
        else {
          childIDs[i] = null;
        }
      }
    }
    else
      newNode.isLeaf = true;

    // set numKeys
    newNode.numKeys = m;
    this.numKeys = m;

    if (DEBUG) {
      msg.append("\n this    aft ");
      msg.append(Arrays.asList(this.data));
      msg.append(Arrays.asList(this.childIDs));
      msg.append("\n newNode aft ");
      msg.append(Arrays.asList(newNode.data));
      msg.append(Arrays.asList(newNode.childIDs));
    }

    file.writePage(this);
    file.writePage(parent);
    file.writePage(newNode);

    if (DEBUG) {
      logger.fine(msg.toString());
    }

    if (parent.numKeys > 2 * m) parent.overflowTreatment();
  }

  /**
   * Splits the root node.
   */
  private void splitRoot() {
    if (numKeys < 2 * m)
      throw new IllegalStateException("This node has no overflow!");

    if (parentID != null && this.nodeID != 0)
      throw new IllegalStateException("This node is not root!");

    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
    }

    BTreeNode<K, V> left = new BTreeNode<K, V>(m, nodeID, file);
    BTreeNode<K, V> right = new BTreeNode<K, V>(m, nodeID, file);
    file.writePage(left);
    file.writePage(right);

    // move the data
    for (int i = 0; i < m; i++) {
      left.data[i] = data[i];
      right.data[i] = data[m + i + 1];
    }

    // root is not a leaf: move the children, set the parent ids and the leaf flags
    if (!isLeaf) {
      for (int i = 0; i <= m; i++) {
        BTreeNode<K, V> leftChild = file.readPage(childIDs[i]);
        left.childIDs[i] = leftChild.nodeID;
        leftChild.parentID = left.nodeID;
        file.writePage(leftChild);

        BTreeNode<K, V> rightChild = file.readPage(childIDs[m + i + 1]);
        right.childIDs[i] = rightChild.nodeID;
        rightChild.parentID = right.nodeID;
        file.writePage(rightChild);
      }
      left.isLeaf = false;
      right.isLeaf = false;
    }
    // else: root is no leaf any more
    else {
      isLeaf = false;
    }

    // only one key left in this root
    data[0] = data[m];
    numKeys = 1;
    left.numKeys = m;
    right.numKeys = m;
    for (int i = 1; i <= 2 * m; i++) {
      data[i] = null;
      childIDs[i + 1] = null;
    }
    childIDs[0] = left.nodeID;
    childIDs[1] = right.nodeID;

    if (DEBUG) {
      msg.append("\n left.data ");
      msg.append(Arrays.asList(left.data));
      msg.append("\n left.children ");
      msg.append(Arrays.asList(left.childIDs));
      msg.append("\n right.data ");
      msg.append(Arrays.asList(right.data));
      msg.append("\n right.children ");
      msg.append(Arrays.asList(right.childIDs));
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
    }

    file.writePage(left);
    file.writePage(right);
    file.writePage(this);

    if (DEBUG) {
      logger.fine(msg.toString());
    }
  }

  /**
   * Treatment of an underflow in this node. Try to merge this node with left or right sibling if possible.
   * If not, merge to one node. The merge can continue recursively in parent node.
   * Underflow and merging can propagate up to the root and reduce the number of the levels of this BTree.
   */
  private void underflowTreatment() {
    StringBuffer msg = new StringBuffer();

    if (numKeys > m)
      throw new IllegalStateException("This node has no underflow!");

    // this node is the root
    if (parentID == null && nodeID == 0) {
      if (numKeys > 0) return;
      mergeRoot();
      return;
    }

    // look for the index of this node in parent
    BTreeNode<K, V> parent = file.readPage(parentID);
    int parentIndex = 0;
    while (parent.childIDs[parentIndex] != this.nodeID) parentIndex++;
    if (DEBUG) {
      msg.append("\n parentNode: ");
      msg.append(parent);
      msg.append(", index: ");
      msg.append(parentIndex);
    }

    // get the brother node and try to adjust
    if (parentIndex > 0) {
      BTreeNode<K, V> leftBrother = file.readPage(parent.childIDs[parentIndex - 1]);
      // adjust with left brother
      if (leftBrother.numKeys > m) {
        BTreeData<K, V> parentData = parent.data[parentIndex - 1];
        Integer leftID = leftBrother.childIDs[leftBrother.numKeys];
        if (leftID != null) {
          BTreeNode<K, V> leftChild = file.readPage(leftID);
          leftChild.parentID = nodeID;
          file.writePage(leftChild);
        }
        shiftRight(0, parentData, leftID, true);
        parent.data[parentIndex - 1] = leftBrother.shiftLeft(leftBrother.numKeys - 1, false);

        file.writePage(this);
        file.writePage(leftBrother);
        file.writePage(parent);
        if (DEBUG) {
          logger.fine(msg.toString());
        }

        return;
      }
    }
    if (parentIndex < parent.numKeys) {
      BTreeNode<K, V> rightBrother = file.readPage(parent.childIDs[parentIndex + 1]);
      // adjust with right brother
      if (rightBrother.numKeys > m) {
        BTreeData<K, V> parentData = parent.data[parentIndex];
        Integer rightID = rightBrother.childIDs[0];
        if (rightID != null) {
          BTreeNode<K, V> rightChild = file.readPage(rightID);
          rightChild.parentID = nodeID;
          file.writePage(rightChild);
        }
        shiftRight(numKeys, parentData, rightID, false);
        parent.data[parentIndex] = rightBrother.shiftLeft(0, true);

        file.writePage(this);
        file.writePage(rightBrother);
        file.writePage(parent);
        if (DEBUG) {
          logger.fine(msg.toString());
        }

        return;
      }
    }

    // merge with siblings
    parent.mergeChild(parentIndex);
    logger.info(msg.toString());

    if (parent.numKeys < m)
      parent.underflowTreatment();
  }

  /**
   * Merges the root node.
   */
  private void mergeRoot() {
    if (numKeys > 0)
      throw new IllegalStateException("This node has no underflow!");

    if (parentID != null && this.nodeID != 0)
      throw new IllegalStateException("This node is not root!");

    if (isLeaf) {
      return;
    }


    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
    }

    BTreeNode<K, V> left = file.readPage(this.childIDs[0]);

    // move the data
    for (int i = 0; i < left.numKeys; i++) {
      data[numKeys++] = left.data[i];
    }
    if (this.numKeys > 2 * m)
      throw new IllegalStateException("Should never happen!");

    // root is now a leaf
    if (left.isLeaf) {
      isLeaf = true;
      this.childIDs[0] = null;
    }

    // root is not a leaf: move the children, set the parent ids and the leaf flags
    if (!isLeaf) {
      for (int i = 0; i <= left.numKeys; i++) {
        BTreeNode<K, V> child = file.readPage(left.childIDs[i]);
        childIDs[i] = child.nodeID;
        child.parentID = nodeID;
        file.writePage(child);
      }
    }

    if (DEBUG) {
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
    }

    file.deletePage(left.nodeID);
    file.writePage(this);

    if (DEBUG) {
      logger.fine(msg.toString());
    }
  }

  /**
   * Merges the entries of the child node at the specified index
   * with the entries of one of its sibling nodes.
   */
  private void mergeChild(int index) {
    BTreeNode<K, V> node = file.readPage(childIDs[index]);

    if (index > 0) {
      BTreeNode<K, V> leftBrother = file.readPage(childIDs[index - 1]);
      for (int i = leftBrother.numKeys; i >= 0; i--) {
        BTreeData<K, V> shiftData = i == leftBrother.numKeys ?
                                    shiftLeft(index - 1, true) :
                                    leftBrother.data[i];
        Integer shiftChildID = leftBrother.childIDs[i];
        if (shiftChildID != null) {
          BTreeNode<K, V> shiftChild = file.readPage(shiftChildID);
          shiftChild.parentID = node.nodeID;
          file.writePage(shiftChild);
        }
        node.shiftRight(0, shiftData, shiftChildID, true);
      }

      file.deletePage(leftBrother.nodeID);
      file.writePage(node);
      file.writePage(this);
    }

    else {
      BTreeNode<K, V> rightBrother = file.readPage(childIDs[index + 1]);
      for (int i = node.numKeys; i >= 0; i--) {
        BTreeData<K, V> shiftData = i == node.numKeys ?
                                    shiftLeft(0, true) :
                                    node.data[i];
        Integer shiftChildID = node.childIDs[i];
        if (shiftChildID != null) {
          BTreeNode<K, V> shiftChild = file.readPage(shiftChildID);
          shiftChild.parentID = rightBrother.nodeID;
          file.writePage(shiftChild);
        }
        rightBrother.shiftRight(0, shiftData, shiftChildID, true);
      }
      file.deletePage(node.nodeID);
      file.writePage(rightBrother);
      file.writePage(this);
    }

    childIDs[numKeys + 1] = null;
  }

  /**
   * Moves the data behind the startpos one position right
   * and inserts the specified data into the gap.
   * This method can only be applied to leaf nodes.
   *
   * @param startPos the start position to shift
   * @param data     the data to be inserted
   */
  private void shiftRight(int startPos, BTreeData<K, V> data) {
    if (!isLeaf)
      throw new IllegalStateException("Node is not a leaf!");

    // shift right
    for (int i = numKeys; i > startPos; i--) {
      this.data[i] = this.data[i - 1];
    }

    // insert
    this.data[startPos] = data;
    numKeys++;
  }

  /**
   * Moves the data and the children behind the startpos one position right
   * and inserts the specified data and the specified childID into the gap.
   * If the flag rightChild is set, the childID will be inserted at startPos,
   * otherwise it will be inserted at startPos + 1
   *
   * @param startPos   the start position to shift
   * @param data       the data to be inserted
   * @param childID    the id of the chikld to be inserted
   * @param rightChild a flag that indicates the position of the childID:
   *                   if rightChild is true, the childID will be inserted at startPos,
   *                   otherwise it will be inserted at startPos + 1
   */
  private void shiftRight(int startPos, BTreeData<K, V> data, Integer childID, boolean rightChild) {
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
      msg.append("\n this.children ");
      msg.append(Arrays.asList(this.childIDs));
      msg.append("\n shift right ");
      msg.append(startPos);
    }

    // shift right
    for (int i = numKeys; i > startPos; i--) {
      this.data[i] = this.data[i - 1];
      childIDs[i + 1] = childIDs[i];
    }
    if (rightChild)
      childIDs[startPos + 1] = childIDs[startPos];

    // insert
    this.data[startPos] = data;
    if (rightChild)
      this.childIDs[startPos] = childID;
    else
      this.childIDs[startPos + 1] = childID;
    numKeys++;

    if (DEBUG) {
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
      msg.append("\n this.children ");
      msg.append(Arrays.asList(this.childIDs));
      logger.fine(msg.toString());
    }
  }

  /**
   * Moves the data and the children behind the startpos one position left
   * and returns the removed data.
   *
   * @param startPos  the start position to shift
   * @param leftChild a flag that indicates the position of the childID to be removed:
   *                  if leftChild is true, the childID will be deleted at startPos,
   *                  otherwise it will be deleted at startPos + 1
   * @return the removed data
   */
  private BTreeData<K, V> shiftLeft(int startPos, boolean leftChild) {
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
      msg.append("\n this.children ");
      msg.append(Arrays.asList(this.childIDs));
      msg.append("\n shift left ");
      msg.append(startPos);
    }

    BTreeData<K, V> data = this.data[startPos];

    for (int i = startPos; i < numKeys; i++) {
      this.data[i] = this.data[i + 1];
      if (!leftChild && i == startPos) continue;
      this.childIDs[i] = this.childIDs[i + 1];
    }
    this.data[numKeys] = null;
    this.childIDs[numKeys] = null;
    numKeys--;

    if (DEBUG) {
      msg.append("\n this.data ");
      msg.append(Arrays.asList(this.data));
      msg.append("\n this.children ");
      msg.append(Arrays.asList(this.childIDs));
      logger.fine(msg.toString());
    }
    return data;
  }

  public void test() {
    int numK = 0;
    for (int i = 0; i < 2 * m; i++) {
      if (data[i] != null) {
        numK++;
      }
    }

    int numC = 0;
    for (int i = 0; i < 2 * m + 1; i++) {
      if (childIDs[i] != null) {
        numC++;
      }
    }

    if (numK != numKeys || (!isLeaf && numC != numKeys + 1)) {
      System.out.println(this);
      System.out.println("data " + Arrays.asList(this.data));
      System.out.println("childIDs " + Arrays.asList(this.childIDs));
      System.out.println("numKeys : " + this.numKeys);
      System.out.println("numK : " + numK);
      System.out.println("numC : " + numC);
      throw new IllegalStateException("XXXXXXXXXX");
    }

    if (isLeaf) return;

    for (int i = 0; i <= numKeys; i++) {
      BTreeNode<K, V> child = file.readPage(childIDs[i]);
      if (child.parentID != this.nodeID)
        throw new IllegalStateException("Wrong parentID: " + child + " soll parent: " + nodeID + " ist parent: " + child.parentID);

      if (i < numKeys) {
        BTreeData<K, V> data = this.data[i];
        if (child.data[0].key.compareTo(data.key) > 0) {
          System.out.println("child.data[0].key " + child.data[0].key);
          System.out.println("data.key " + data.key);
          throw new IllegalStateException("XXX");
        }
        if (child.data[child.numKeys - 1].key.compareTo(data.key) > 0)
          throw new IllegalStateException("XXX");
      }

      child.test();
    }
  }

}
