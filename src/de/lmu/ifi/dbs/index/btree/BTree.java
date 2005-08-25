/***********************************************************************
 * B-Tree
 *
 *	by L.Horisberger & G.Schweizer
 * last change: 26.02.1998 by horil
 **/
package de.lmu.ifi.dbs.index.btree;

import de.lmu.ifi.dbs.persistent.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Implementation of a B-Tree.
 * <p/>
 * BTrees of order m have following properties: <br>
 * Root is  either a leaf or has between 2 and m children. <br>
 * All nonleaf nodes but the root have between ceil(m/2) and m children. <br>
 * All leaves are at same depth. <br>
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BTree<K extends Comparable<K> & Serializable, V extends Serializable> {
  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  private static Level level = Level.OFF;

  /**
   * The file storing the BTree.
   */
  private PageFile<BTreeNode<K, V>> file;

  /**
   * Creates a new BTree with the specified parameters. The BTree will be hold in main memory.
   *
   * @param m         the order of the BTree
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   */
  public BTree(int m, int pageSize, int cacheSize) {
    initLogger();
    if (m < 1) throw new IllegalArgumentException("Parameter m has to be greater than 0!");

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
    initLogger();

    // init the file
    this.file = new PersistentPageFile<BTreeNode<K, V>>(new DefaultPageHeader(),
                                                        cacheSize,
                                                        new LRUCache<BTreeNode<K, V>>(),
                                                        fileName);

    if (getRoot() == null) {
      throw new IllegalArgumentException("No root specified in File " + fileName);
    }

  }

  /**
   * Creates a new BTree with the specified parameters. The BTree will be saved persistently
   * in the specified file.
   *
   * @param m         the order of the BTree
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param fileName  the name of the file storing this BTree.
   */
  public BTree(int m, int pageSize, int cacheSize, String fileName) {
    if (m < 1) throw new IllegalArgumentException("Parameter m has to be greater than 0!");

    initLogger();

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
    StringBuffer msg = new StringBuffer();

    BTreeData<K, V> data = new BTreeData<K, V>(key, value);
    msg.append("INSERT ");
    msg.append(data);

    // search for right node
    BTreeNode<K, V> node = getRoot();
    msg.append("\nnode ");
    msg.append(node);
    msg.append(" ");
    msg.append(Arrays.asList(node.data));
    msg.append(" ");
    msg.append(node.isLeaf);

    // is node already a leaf?
    while (!node.isLeaf) {
      int i = 0;
      // go on, until key > data[i].key
      while ((i < node.numKeys) && (data.key.compareTo(node.data[i].key) > 0)) {
        msg.append("\n");
        msg.append(data.key);
        msg.append(" > ");
        msg.append(node.data[i].key);
        i++;
      }

      // key already exists
      if ((i < node.numKeys) && data.key.compareTo(node.data[i].key) == 0) {
        msg.append("\nKey already exists in node ");
        msg.append(node);
        node.data[i] = data;
        logger.info(msg.toString());
        return;
      }

      node = file.readPage(node.childIDs[i]);
      msg.append("\nnode ");
      msg.append(node);
      msg.append(" ");
      msg.append(Arrays.asList(node.data));
      msg.append(" ");
      msg.append(node.isLeaf);
    }

    // insert
    node.insert(data);
    msg.append("\nStructure \n");
    msg.append(this.printStructure());

    logger.info(msg.toString());
  }

  /**
   * Deletes and returns the object with the specified key from this BTree.
   *
   * @param key the key of the object to be deleted
   * @return the deleted object
   */
  public BTreeData<K,V> delete(K key) {
    StringBuffer msg = new StringBuffer();
    msg.append("\n DELETE ");
    msg.append(key);

    // search for right node
    SearchResult<K, V> tmpResult = search(getRoot(), key);
    if (tmpResult == null) return null;

    BTreeNode<K, V> delNode = tmpResult.getNode();
    int keyIndex = tmpResult.getKeyIndex();

    msg.append("\n");
    msg.append(tmpResult);
    logger.info(msg.toString());

    return delNode.delete(keyIndex);
  }

  /**
   * Returns the object with the minimum key.
   *
   * @return the object with twith the minimum key
   */
  public BTreeData<K,V> getMinimum() {
    BTreeNode<K,V> node = getRoot();
    while (! node.isLeaf) {
      node = file.readPage(node.childIDs[0]);
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
    SearchResult<K, V> result = search(getRoot(), key);
    if (result == null)
      return null;
    else
      return (result.getNode().data[result.getKeyIndex()]).value;
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
   * Closes this BTree and writes all nodes to file.
   */
  public void close() {
    file.close();
  }

  /**
   * Returns the I/O-Access of this file.
   */
  public int getIOAccess() {
    return file.getIOAccess();
  }

  /**
   * Recursive method to search for the object with the specified key in the given subtree.
   * First call should be with the root of this BTree.
   *
   * @param node a subtree of this BTree
   * @param key  the key to search for
   * @return the search result: encapsulates the node and the index of the object to find
   */
  private SearchResult<K, V> search(BTreeNode<K, V> node, K key) {
    StringBuffer msg = new StringBuffer();
    msg.append("\n search in node ");
    msg.append(node);
    msg.append(" for key ");
    msg.append(key);

    if ((node == null) || (node.numKeys < 1)) {
      msg.append("\n Key not in tree.");
      logger.info(msg.toString());
      return null;
    }

    // key < k_1
    if (key.compareTo(node.data[0].key) < 0) {
      msg.append("\n   ").append(key).append(" < ").append(node.data[0].key);
      if (!node.isLeaf) {
        BTreeNode<K, V> child = file.readPage(node.childIDs[0]);
        logger.info(msg.toString());
        return search(child, key);
      }
      else
        return null;
    }

    // key > k_numEntries
    if (key.compareTo(node.data[node.numKeys - 1].key) > 0) {
      msg.append("\n   ").append(key).append(" > ").append(node.data[node.numKeys - 1].key);
      if (!node.isLeaf) {
        BTreeNode<K, V> child = file.readPage(node.childIDs[node.numKeys]);
        logger.info(msg.toString());
        return search(child, key);
      }
      else
        return null;
    }

    // determine index: go on until key > k_i+1
    int i = 0;
    while ((i < node.numKeys - 1) && (key.compareTo(node.data[i].key) > 0)) i++;

    // found
    if (key.compareTo(node.data[i].key) == 0) {
      msg.append("\n   ").append(key).append(" == ").append(node.data[i].key).append(" ( ").append(new SearchResult<K, V>(node, i)).append(")");
      logger.info(msg.toString());
      return new SearchResult<K, V>(node, i);
    }

    // k_i < key < k_i+1
    msg.append("\n   ").append(node.data[i - 1].key).append(" < ").append(key).append(" < ").append(node.data[i].key);
    logger.info(msg.toString());
    if (!node.isLeaf) {
      BTreeNode<K, V> child = file.readPage(node.childIDs[i]);
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
    if ((node == null) || (node.numKeys == 0)) return;

    if (node.childIDs[0] != null) {
      BTreeNode<K, V> child = file.readPage(node.childIDs[0]);
      print(child, result);
    }

    for (int i = 0; i < node.numKeys; i++) {
      result.append("Key: ");
      result.append(node.data[i].key);
      result.append("   \tValue: ");
      result.append(node.data[i].toString());
      result.append("\n");

      if (node.childIDs[i] != null) {
        BTreeNode<K, V> child = file.readPage(node.childIDs[i + 1]);
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
    if (node.numKeys < 1) {
      result.append("ERROR: printStructure: empty node!");
      return;
    }

    // ---- indent
    for (int i = 0; i < level; i++)
      result.append("  ");
    // ---- node-ID
    result.append(node.nodeID).append(":");
    // ---- print entries
    result.append(Arrays.asList(node.data));
//    for (int i = 0; i < node.numKeys; i++) {
//      result.append(" ");
//      result.append(node.data[i].key);
//    }
    result.append("\n");
    // -- print subtrees -----
    for (int i = 0; i <= node.numKeys; i++) {
      if (node.childIDs[i] != null) {
        BTreeNode<K, V> child = file.readPage(node.childIDs[i]);
        printStructure(child, level + 1, result);
      }
    }
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
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
   * A dummy class for saving the search result
   */
  class SearchResult<K extends Comparable<K> & Serializable, V extends Serializable> {
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
    BTree<Integer, Integer> tree = new BTree<Integer, Integer>(300, "elkilein");      // Typ (2,h) B-Baum
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
      tree.insert(values[i], i);
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
}
