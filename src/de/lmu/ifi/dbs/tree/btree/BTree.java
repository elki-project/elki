/***********************************************************************
 * B-Tree
 *
 *	by L.Horisberger & G.Schweizer
 * last change: 26.02.1998 by horil
 **/
package de.lmu.ifi.dbs.tree.btree;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.persistent.DefaultPageHeader;
import de.lmu.ifi.dbs.persistent.LRUCache;
import de.lmu.ifi.dbs.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.persistent.PersistentPageFile;
import de.lmu.ifi.dbs.utilities.output.ObjectPrinter;

import java.io.Externalizable;
import java.io.PrintStream;
import java.util.Arrays;

// ELKITODO : place in package index?

/**
 * Implementation of a B-Tree.
 * <p/>
 * BTrees of order m have following properties: <br>
 * Root is  either a leaf or has between 2 and 2m children. <br>
 * All nonleaf nodes but the root have between m and 2m children. <br>
 * All leaves are at same depth. <br>
 *
 * @author Elke Achtert 
 * 
 */
public class BTree<K extends Comparable<K> & Externalizable, V extends Externalizable>
    extends AbstractLoggable {

  /**
   * The file storing the BTree.
   */
  private PageFile<BTreeNode<K, V>> file;

  public BTree() {
    super(LoggingConfiguration.DEBUG);
  }

  /**
   * Creates a new BTree with the specified parameters. The BTree will be hold in main memory.
   *
   * @param keySize   the size of a key in Bytes
   * @param valueSize the size of a value in Bytes
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   */
  public BTree(int keySize, int valueSize, int pageSize, int cacheSize) {
    this();
    int m = determineOrder(pageSize, keySize, valueSize);

    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nkeysize   " + keySize);
      msg.append("\nvalueSize " + valueSize);
      msg.append("\npageSize  " + pageSize);
      msg.append("\ncacheSize " + cacheSize);
      msg.append("\nm         " + m);
      debugFine(msg.toString());
    }

    // init the file
    this.file = new MemoryPageFile<BTreeNode<K, V>>(pageSize, cacheSize,
                                                    new LRUCache<BTreeNode<K, V>>());

    // create a new root
    BTreeNode<K, V> root = new BTreeNode<K, V>(m, null, file);
    file.writePage(root);
  }

  /**
   * Creates a BTree with the specified parameters from an existing persistent file.
   *
   * @param cacheSize the size of the cache in Bytes
   * @param fileName  the name of the file storing this BTree.
   */
  public BTree(int cacheSize, String fileName) {
    this();
    // init the file
    this.file = new PersistentPageFile<BTreeNode<K, V>>(new DefaultPageHeader(),
                                                        cacheSize,
                                                        new LRUCache<BTreeNode<K, V>>(),
                                                        fileName);

    if (getRoot() == null) {
      throw new IllegalArgumentException("No root specified in file " + fileName);
    }

  }

  /**
   * Creates a new BTree with the specified parameters. The BTree will be saved persistently
   * in the specified file.
   *
   * @param keySize   the size of a key in Bytes
   * @param valueSize the size of a value in Bytes
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param fileName  the name of the file storing this BTree.
   */
  public BTree(int keySize, int valueSize, int pageSize, int cacheSize, String fileName) {
    this();
    int m = determineOrder(pageSize, keySize, valueSize);

    this.file = new PersistentPageFile<BTreeNode<K, V>>(new DefaultPageHeader(pageSize),
                                                        cacheSize,
                                                        new LRUCache<BTreeNode<K, V>>(),
                                                        fileName);

    BTreeNode<K, V> root = new BTreeNode<K, V>(m, null, file);
    file.writePage(root);
  }

  /**
   * Inserts a new key - value pair in this BTree.
   *
   * @param key   the key of the value to be inserted
   * @param value the value to be inserted
   */
  public void insert(K key, V value) {
    insert(new BTreeData<K, V>(key, value));
  }

  /**
   * Inserts a new key - value pair in this BTree.
   *
   * @param data the data object to be inserted
   */
  public void insert(BTreeData<K, V> data) {
    StringBuffer msg = new StringBuffer();

    if (this.debug) {
      msg.append("INSERT ").append(data);
    }

    // search for right node
    BTreeNode<K, V> node = getRoot();
    if (this.debug) {
      msg.append("\nnode ").append(node).append(" ");
      msg.append(Arrays.asList(node.getData())).append(" ").append(node.isLeaf());
    }

    // is node already a leaf?
    while (!node.isLeaf()) {
      int i = 0;
      // go on, until key > data[i].key
      while ((i < node.getNumKeys()) && (data.key.compareTo(node.getData(i).key) > 0)) {
        if (this.debug) {
          msg.append("\n").append(data.key).append(" > ").append(node.getData(i).key);
        }
        i++;
      }

      // key already exists
      if ((i < node.getNumKeys()) && data.key.compareTo(node.getData(i).key) == 0) {
        if (this.debug) {
          msg.append("\nKey already exists in node ").append(node);
          debugFine(msg.toString());
        }
        node.setData(data, i);
        return;
      }

      node = file.readPage(node.getChildID(i));
      if (this.debug) {
        msg.append("\nnode ").append(node).append(" ");
        msg.append(Arrays.asList(node.getData())).append(" ").append(node.isLeaf());
      }
    }

    // insert
    node.insert(data);
    if (this.debug) {
      msg.append("\nStructure \n").append(this.printStructure());
      debugFine(msg.toString());
    }
  }

  /**
   * Deletes and returns the object with the specified key from this BTree.
   *
   * @param key the key of the object to be deleted
   * @return the deleted object
   */
  public BTreeData<K, V> delete(K key) {
    StringBuffer msg = new StringBuffer();

    if (this.debug) {
      msg.append("\n DELETE ");
      msg.append(key);
    }

    // search for right node
    SearchResult tmpResult = search(getRoot(), key);
    if (tmpResult == null) return null;

    BTreeNode<K, V> delNode = tmpResult.getNode();
    int keyIndex = tmpResult.getKeyIndex();

    if (this.debug) {
      msg.append("\n");
      msg.append(tmpResult);
      debugFine(msg.toString());
    }

    return delNode.delete(keyIndex);
  }

  /**
   * Returns the object with the minimum key.
   *
   * @return the object with twith the minimum key
   */
  public BTreeData<K, V> getMinimum() {
    BTreeNode<K, V> node = getRoot();
    while (!node.isLeaf()) {
      node = file.readPage(node.getChildID(0));
    }
    return node.delete(0);
  }

  /**
   * Returns the object with the specified key.
   *
   * @param key the key of the object to be returned
   * @return the object with the specified key or null if the key does not exists
   */
  public V search(K key) {
    SearchResult result = search(getRoot(), key);
    if (result == null)
      return null;
    else
      return (result.getNode().getData(result.getKeyIndex())).value;
  }

  /**
   * Returns the object with the specified key and marks the node containing
   * the object as dirty.
   *
   * @param key the key of the object to be returned
   * @return the object with the specified key or null if the key does not exists
   */
  public V searchForUpdate(K key) {
    SearchResult result = search(getRoot(), key);
    if (result == null)
      return null;
    else {
      result.getNode().setDirty(true);
      return (result.getNode().getData(result.getKeyIndex())).value;
    }
  }

  /**
   * Returns a string representation of this BTree.
   *
   * @return a string representation of this BTree
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    print(getRoot(), result);
    return result.toString();
  }

  /**
   * Returns a string representation of the hierarchy of this BTree.
   * Each node is printed with its id and its values. The root has id 1, all other
   * nodes inherit the id of its parent node and appen their index.
   *
   * @return a string representation of the hierarchy of this BTree
   */
  public String printStructure() {
    StringBuffer result = new StringBuffer();
    printStructure(getRoot(), 0, result);
    return result.toString();
  }

  /**
   * Writes the content of this BTree to the specified stream.
   * Each data entry is printed in a separate line.
   *
   * @param outStream the stream to write into
   * @param printer   the object printer that provides the print data
   *                  for each objects
   */
  public void writeData(PrintStream outStream, ObjectPrinter<BTreeData<K, V>> printer) {
    writeData(getRoot(), outStream, printer);
  }

  /**
   * Closes this BTree and writes all nodes to file.
   */
  public void close() {
    file.close();
  }

  /**
   * Returns the physical read access of this B-Tree.
   *
   * @return the physical read access of this B-Tree
   */
  public long getPhysicalReadAccess() {
    return file.getPhysicalReadAccess();
  }

  /**
   * Returns the physical write access of this B-Tree.
   *
   * @return the physical write access of this B-Tree
   */
  public long getPhysicalWriteAccess() {
    return file.getPhysicalWriteAccess();
  }

  /**
   * Returns the logical page access of this B-Tree.
   *
   * @return the logical page access of this B-Tree
   */
  public long getLogicalPageAccess() {
    return file.getLogicalPageAccess();
  }

  /**
   * Resets the counters for page access.
   */
  public void resetPageAccess() {
    file.resetPageAccess();
  }

  /**
   * Recursive method to search for the object with the specified key in the given subtree.
   * First call should be with the root of this BTree.
   *
   * @param node a subtree of this BTree
   * @param key  the key to search for
   * @return the search result: encapsulates the node and the index of the object to find
   */
  private SearchResult search(BTreeNode<K, V> node, K key) {
    StringBuffer msg = new StringBuffer();
    if (this.debug) {
      msg.append("\n search in node ").append(node);
      msg.append(" for key ").append(key);
    }

    if ((node == null) || (node.getNumKeys() < 1)) {
      if (this.debug) {
        msg.append("\n Key not in tree.");
        debugFine(msg.toString());
      }
      return null;
    }

    // key < k_1
    if (key.compareTo(node.getData(0).key) < 0) {
      if (this.debug) {
        msg.append("\n   ").append(key).append(" < ").append(node.getData(0).key);
        debugFine(msg.toString());
      }
      if (!node.isLeaf()) {
        BTreeNode<K, V> child = file.readPage(node.getChildID(0));
        return search(child, key);
      }
      else
        return null;
    }

    // key > k_numEntries
    if (key.compareTo(node.getData(node.getNumKeys() - 1).key) > 0) {
      if (this.debug) {
        msg.append("\n   ").append(key).append(" > ").append(node.getData(node.getNumKeys() - 1).key);
        debugFine(msg.toString());
      }
      if (!node.isLeaf()) {
        BTreeNode<K, V> child = file.readPage(node.getChildID(node.getNumKeys()));
        return search(child, key);
      }
      else
        return null;
    }

    // determine index: go on until key > k_i+1
    int i = 0;
    while ((i < node.getNumKeys() - 1) && (key.compareTo(node.getData(i).key) > 0)) i++;

    // found
    if (key.compareTo(node.getData(i).key) == 0) {
      if (this.debug) {
        msg.append("\n   ").append(key).append(" == ").append(node.getData(i).key).append(" ( ").append(new SearchResult(node, i)).append(")");
        debugFine(msg.toString());
      }
      return new SearchResult(node, i);
    }

    // k_i < key < k_i+1
    if (this.debug) {
      msg.append("\n   ").append(node.getData(i - 1).key).append(" < ").append(key).append(" < ").append(node.getData(i).key);
      debugFine(msg.toString());
    }
    if (!node.isLeaf()) {
      BTreeNode<K, V> child = file.readPage(node.getChildID(i));
      return search(child, key);
    }
    else
      return null;
  }

  /**
   * Writes a string represenation of the subtree with the specified root
   * in the given StringBuffer.
   *
   * @param node   the root of the subtree
   * @param result the string buffer containing the print result
   */
  private void print(BTreeNode<K, V> node, StringBuffer result) {
    if ((node == null) || (node.getNumKeys() == 0)) return;

    if (node.getChildID(0) != null) {
      BTreeNode<K, V> child = file.readPage(node.getChildID(0));
      print(child, result);
    }

    for (int i = 0; i < node.getNumKeys(); i++) {
      result.append("Key: ");
      result.append(node.getData(i).key);
      result.append("   \tValue: ");
      result.append(node.getData(i).toString());
      result.append("\n");

      if (node.getChildID(i) != null) {
        BTreeNode<K, V> child = file.readPage(node.getChildID(i + 1));
        print(child, result);
      }
    }
  }

  /**
   * Writes a string represenation of the hierarchy of the subtree with the specified root
   * in the given StringBuffer.
   *
   * @param node   the root of the subtree
   * @param level  the level of the subtree
   * @param result the StringBuffer to write into
   */
  private void printStructure(BTreeNode<K, V> node, int level, StringBuffer result) {
    node.test();
    if (node.getNumKeys() < 1) {
      result.append("ERROR: printStructure: empty node!");
      return;
    }

    // ---- indent
    for (int i = 0; i < level; i++)
      result.append("  ");
    // ---- node-ID
    result.append(node.getID()).append(":");
    // ---- print entries
    result.append(Arrays.asList(node.getData()));
//    for (int i = 0; i < node.numKeys; i++) {
//      result.append(" ");
//      result.append(node.data[i].key);
//    }
    result.append("\n");
    // -- print subtrees -----
    for (int i = 0; i <= node.getNumKeys(); i++) {
      if (node.getChildID(i) != null) {
        BTreeNode<K, V> child = file.readPage(node.getChildID(i));
        printStructure(child, level + 1, result);
      }
    }
  }

  /**
   * Writes the content of of the subtree with the specified root to the specified stream.
   * Each data entry is printed in a separate line.
   *
   * @param node      the root of the subtree
   * @param outStream the stream to write into
   * @param printer   the object printer that provides the print data
   *                  for each objects
   */
  private void writeData(BTreeNode<K, V> node, PrintStream outStream, ObjectPrinter<BTreeData<K, V>> printer) {
    // ---- print data
    for (BTreeData<K, V> data : node.getData())
      if (data != null) {
        outStream.println(printer.getPrintData(data));
      }

    // -- print subtrees -----
    for (int i = 0; i <= node.getNumKeys(); i++) {
      if (node.getChildID(i) != null) {
        BTreeNode<K, V> child = file.readPage(node.getChildID(i));
        writeData(child, outStream, printer);
      }
    }
  }

  /**
   * Returns the root of this BTree.
   *
   * @return the root of this BTree
   */
  private BTreeNode<K, V> getRoot() {
    return file.readPage(0);
  }

  /**
   * Class SearchResult
   * A helper class for saving the search result
   */
  class SearchResult {
    private BTreeNode<K, V> node;
    private int keyIndex;

    SearchResult(BTreeNode<K, V> btnode, int keyIndex) {
      this.node = btnode;
      this.keyIndex = keyIndex;
    }

    int getKeyIndex() {
      return keyIndex;
    }

    BTreeNode<K, V> getNode() {
      return node;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
      return "Node: " + node + ", keyIndex: " + keyIndex;
    }
  }

  public static void main(String[] args) {
    int m = 1;
//    BTree<Integer, Integer> tree = new BTree<Integer, Integer>(m, 300, 300, "elkilein");      // Typ (2,h) B-Baum
    BTree<DefaultKey, DefaultKey> tree = new BTree<DefaultKey, DefaultKey>(4, 4, 60, 80000);      // Typ (2,h) B-Baum
//    BTree<Integer, String> tree = new BTree<Integer, String>(m, 50, 5000, null);      // Typ (2,h) B-Baum
    int[] values = {104, 56, 222, 12, 58, 180, 301,
        1, 93, 121, 254, 420, 63, 5, 72,
        245, 81, 113, 4, 72, 83, 60, 271, 567, 234, 7438, 2, 9, 53, 54, 55, 67, 32, 33, 45, 101, 102,
        103, 104, 105, 789, 234, 235, 278}; // Werte, mit denen der
    // Baum erzeugt wird

    // Erzeuge Baum
    System.out.println("Erzeuge B-Baum der Klasse (" + m + ",h) ...");
    for (int i = 0; i < values.length; i++) {
      System.out.println("\nXXXXXXXX insert " + values[i]);
      tree.insert(new DefaultKey(values[i]), new DefaultKey(i));
//      tree.insert(values[i], "Elki " + i);
//      System.out.println(tree.toString());
      System.out.println(tree.printStructure());
    }
    System.out.println(tree.printStructure());

//    tree.close();
//    if (true) return;

//    for (int i = 0; i < values.length; i++) {
//      System.out.println("\n DELETE " + values[i]);
//      tree.delete(values[i]);
//      System.out.println(tree.toString());
//      System.out.println("\n YYYYYYYYYYYY DELETE " + values[i]);
//      System.out.println(tree.printStructure());
//    }

    // Ausgabe eines Durchlaufs in symmetrischer Ordnung
    System.out.println("\n\nBaumdurchlauf in symmetrischer Ordnung nach einfuegen: ");
    System.out.println(tree);

    // Ausgabe der Baumstruktur
    System.out.println("\nBaumstruktur: ");
    System.out.println(tree.printStructure());

    tree.close();

  }


  /**
   * Determines and returns the order of this B-Tree.
   *
   * @param pageSize  the size of a page in Bytes
   * @param keySize   the size of a key in Bytes
   * @param valueSize the size of a value in Bytes
   * @return the order of this B-Tree
   */
  private int determineOrder(double pageSize, double keySize, double valueSize) {

    // m, nodeID, numKeys, parentID, isLeaf
    double overhead = 17;
    double childIDs = 4;
    // pagesize = overhead + (m+1) * valueSize + (m+1) * keySize + (m+2) * childIDs
    int m = (int) ((pageSize - overhead - 2 * childIDs - keySize - valueSize) / (valueSize + keySize + childIDs));
    if (m < 2) throw new IllegalArgumentException("Parameter pagesize is chosen " +
                                                  "to small!");


    return m / 2;
  }
}
