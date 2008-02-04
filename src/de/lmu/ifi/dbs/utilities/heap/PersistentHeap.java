package de.lmu.ifi.dbs.utilities.heap;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.persistent.DefaultPageHeader;
import de.lmu.ifi.dbs.persistent.LRUCache;
import de.lmu.ifi.dbs.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.persistent.PersistentPageFile;
import de.lmu.ifi.dbs.utilities.Identifiable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

/**
 * Persistent implementation of a heap-based priority queue. The heap is
 * organised according to the natural order of the stored objects. Elements
 * stored in this heap must be instances of <code>PersistentHeapNode<\code>
 * (@see PersistentHeapNode).
 * <p/>
 * This persistent heap has nodes which are instances of <code>Deap<\code>, i.e. the nodes of this
 * heap are again heaps, more precisely a special implementation of MinMaxHeap. The actual
 * elements are stored in those deaps. Only one path of deaps from the root to the last leaf
 * of this heap is cached in main memory, the rest of the nodes is written to disk. The cache path
 * can change during insert / remove operations.
 *
 * @author Elke Achtert 
 */
public class PersistentHeap<K extends Comparable<K> & Serializable, V extends Identifiable & Serializable>
    extends AbstractLoggable implements Heap<K, V> {

  /**
   * The file storing the elements of this heap.
   */
  private final PageFile<Deap<K, V>> file;

  /**
   * The size of a page (deap) in Bytes.
   */
  private final int pageSize;

  /**
   * The number of elements in this heap.
   */
  private int numElements;

  /**
   * The maximum number of deaps in th cache, corresponds to the maximum
   * height of this heap.
   */
  private final int maxCacheSize;

  /**
   * The maximum index of a deap in this heap (corresponds to the maximum
   * number of deaps - 1)
   */
  private final int maxDeapIndex;

  /**
   * The maximum size of a deap.
   */
  private final int maxDeapSize;

  /**
   * The actual height of this heap.
   */
  private int height;

  /**
   * The actual number of Deaps in this heap.
   */
  private int numDeaps;

  /**
   * The actual path of this heap in main memory. This array consists of one
   * path from root to the last leaf.
   */
  private Deap<K, V>[] cachePath;

  /**
   * Creates a new persistent heap with the specified parameters. The heap
   * will be hold in main memory, the i/o-accesses are only simulated.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param nodeSize  the size of a node in Bytes
   */
  public PersistentHeap(int pageSize, int cacheSize, int nodeSize) {
    this(null, pageSize, cacheSize, nodeSize);
  }

  /**
   * Creates a new persistent heap with the specified parameters.
   *
   * @param fileName  the name of the file storing the heap, if this parameter is
   *                  null, the heap will be hold in main memory
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param nodeSize  the size of a node in Bytes
   */
  public PersistentHeap(String fileName, int pageSize, int cacheSize,
                        int nodeSize) {
    super(LoggingConfiguration.DEBUG);
    if (cacheSize <= 0)
      throw new IllegalArgumentException(
          "Cache size must be greater than 0!");

//		initLogger();
    StringBuffer msg = new StringBuffer();

    this.numElements = 0;
    this.pageSize = pageSize;

    // maximum number of deaps in cachePath
    this.maxCacheSize = cacheSize / pageSize;
    if (maxCacheSize <= 0)
      throw new IllegalArgumentException("Cache size of " + cacheSize
                                         + " Bytes is choosen too small for a" + " pagesize of "
                                         + pageSize + " Bytes!");

    // noinspection unchecked
    this.cachePath = new Deap[maxCacheSize];

    // maximum index of a deap
    this.maxDeapIndex = (int) (Math.pow(2, maxCacheSize) - 2);

    // maximum size of a deap
    this.maxDeapSize = pageSize / nodeSize;
    if (maxDeapSize <= 0)
      throw new IllegalArgumentException("Page size of " + pageSize
                                         + " Bytes is choosen too small for a" + " node size of "
                                         + nodeSize + " Bytes!");

    // the actual height and actual number of deaps
    this.height = 0;
    this.numDeaps = 0;

    if (fileName == null) {
      this.file = new MemoryPageFile<Deap<K, V>>(pageSize, maxCacheSize
                                                           * pageSize, new LRUCache<Deap<K, V>>());
    }
    else {
      this.file = new PersistentPageFile<Deap<K, V>>(
          new DefaultPageHeader(pageSize), maxCacheSize * pageSize,
          new LRUCache<Deap<K, V>>(), fileName);
    }

    if (this.debug) {
      msg.append("\n pageSize = ");
      msg.append(pageSize);
      msg.append(" (= 1 deap)");

      msg.append("\n nodeSize = ");
      msg.append(nodeSize);

      msg.append("\n cacheSize = ");
      msg.append(maxCacheSize);
      msg.append(" deaps");

      msg.append("\n maxDeapIndex = ");
      msg.append(maxDeapIndex);

      msg.append("\n maxDeapSize = ");
      msg.append(maxDeapSize);
      debugFine(msg.toString());
    }
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public synchronized void addNode(final HeapNode<K, V> node) {
    if (getIndexOf(node.getValue()) != null)
      throw new IllegalArgumentException("Node " + node
                                         + " already exists in this heap!");

    StringBuffer msg = new StringBuffer();

    // get last deap in cachePath
    Deap<K, V> deap = getLastDeap();

    // cachePath is empty at beginning
    if (deap == null) {
      if (this.debug)
        msg.append("Cache is empty, create new deap!");
      deap = createNewLastDeap();
    }

    // last deap is full
    else if (deap.isFull()) {
      // no more deap fits into cache
      if (deap.getIndex() == maxDeapIndex)
        throw new IllegalArgumentException("Cache is full!");

      // else: create new deap and reorganize cache
      if (this.debug)
        msg.append("Last deap is full, create new deap! (" + size()
                   + ") I/O = " + (getPhysicalReadAccess() + getPhysicalWriteAccess()));
      deap = createNewLastDeap();
    }

    // insert node in last deap
    deap.addNode(node);

    // re-establish heap order
    heapify(deap);

    numElements++;

    if (this.debug) {
      msg.append("\n add ");
      msg.append(node);
      msg.append("\n");
      msg.append(this);
      debugFine(msg.toString());
    }
  }

  /**
   * Retrieves and removes the minimum node of this heap. If the heap is
   * empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public HeapNode<K, V> getMinNode() {
    if (numElements < 1)
      throw new RuntimeException("No elements in priority queue!");

    StringBuffer msg = new StringBuffer();

    // get first deap
    Deap<K, V> deap = getFirstDeap();
    // get first element
    HeapNode<K, V> min = deap.getMinNode();

    // if first deap is empty, adjust recursively with sons
    if (deap.isEmpty()) {
      if (this.debug)
        msg.append("First deap is empty --> adjust it!");
      adjustFirstDeap();
    }

    numElements--;

    if (this.debug) {
      msg.append("\n add ");
      msg.append(min);
      msg.append("\n");
      msg.append(this);
      debugFine(msg.toString());
    }

    return min;
  }

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  public boolean isEmpty() {
    return numElements == 0;
  }

  /**
   * Returns the current index of the specified value in this heap.
   *
   * @param value the value for which the index should be returned
   * @return the current index of the specified value in this heap
   */
  public Integer getIndexOf(V value) {
    Integer index = null;
    Integer deapIndex = null;

    for (int i = 0; i < numDeaps; i++) {
      Deap<K, V> deap = getDeap(i);
      index = deap.getIndexOf(value);
      if (index != null) {
        deapIndex = deap.getIndex();
        break;
      }
    }

    if (index != null && deapIndex != null)
      return deapIndex * numDeaps + index;

    return null;
  }

  /**
   * Returns the node at the specified index.
   *
   * @param index the index of the node to be returned
   * @return the node at the specified index
   */
  public HeapNode<K, V> getNodeAt(int index) {
    int deapIndex = index / numDeaps;
    index = index % numDeaps;

    Deap<K, V> deap = getDeap(deapIndex);
    return deap.getNodeAt(index);
  }

  /**
   * Moves up a node at the specified index until it satisfies the heaporder.
   *
   * @param index the index of the node to be moved up.
   */
  public void flowUp(int index) {
    int deapIndex = index / numDeaps;
    index = index % numDeaps;

    Deap<K, V> deap = getDeap(deapIndex);
    deap.flowUp(index);

    heapify(deap);
  }

  /**
   * Returns the size of this heap.
   *
   * @return the size of this heap
   */
  public int size() {
    return numElements;
  }

  /**
   * Closes this persistent heap.
   */
  public void close() {
    for (Deap<K, V> aCachePath : cachePath) {
      file.writePage(aCachePath);
    }
    file.close();
  }

  /**
   * Returns a string representation of this heap.
   *
   * @return a string representation of this heap
   */
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("*****************\n");
    buffer.append("cache: ");
    buffer.append(Arrays.asList(cachePath));
    buffer.append("\n");

    int pathIndex = 0;
    for (int i = 0; i < numDeaps; i++)
      if (inCache(i)) {
        Deap<K,V> deap = cachePath[pathIndex++];

        buffer.append(deap);
        buffer.append("*(");
        buffer.append(deap.getCacheIndex());
        buffer.append(") --- ");

        if (deap.getCacheIndex() == -1) {
          throw new RuntimeException();
        }
      }
      else {
        Deap<K,V> deap = file.readPage(i);
        buffer.append(deap);
        buffer.append("(");
        buffer.append(deap.getCacheIndex());
        buffer.append(") --- ");

        if (deap.getCacheIndex() != -1) {
          warning(buffer.toString());
          throw new RuntimeException();
        }
      }
    buffer.append("\n\n");
    return buffer.toString();

  }

  /**
   * Returns the physical read I/O-access of this heap.
   *
   * @return the I/O-Access of this heap
   */
  public long getPhysicalReadAccess() {
    return file.getPhysicalReadAccess();
  }

  /**
   * Returns the physical write I/O-access of this heap.
   *
   * @return the I/O-Access of this heap
   */
  public long getPhysicalWriteAccess() {
    return file.getPhysicalWriteAccess();
  }

  /**
   * Returns the logical read I/O-access of this heap.
   *
   * @return the I/O-Access of this heap
   */
  public long getLogicalPageAccess() {
    return file.getLogicalPageAccess();
  }

  /**
   * Resets the I/O-Access of this heap.
   */
  public void resetIOAccess() {
    file.resetPageAccess();
  }

  /**
   * Returns the last deap in the cache path.
   *
   * @return the last deap in the cache path
   */
  private Deap<K, V> getLastDeap() {
    if (height == 0) {
      return null;
    }

    if (this.debug)
      debugFine("height = " + height);

    return cachePath[height - 1];
  }

  /**
   * Returns the first deap in the cache path.
   *
   * @return the first deap in the cache path
   */
  private Deap<K, V> getFirstDeap() {
    return cachePath[0];
  }

  /**
   * Creates and returns a new deap as last element of the cache path.
   *
   * @return a new deap as last element of the cache path
   */
  private Deap<K, V> createNewLastDeap() {
    // determine index of the new deap in the cachePath
    int cacheIndex = level(numDeaps) - 1;

    // create new deap
    Deap<K, V> newDeap = new Deap<K, V>(maxDeapSize, numDeaps, cacheIndex);

    // get old deap from cachePath
    Deap<K, V> oldDeap = cachePath[cacheIndex];

    // insert the new deap into cache
    cachePath[cacheIndex] = newDeap;

    // reorganize th cache: insert parents of newDeap into cachePath
    insertParentToCache(newDeap);

    // oldDeap has been replaced in cache
    if (oldDeap != null) {
      // set cacheIndex of oldDeap to -1 and write oldDeap to disk
      oldDeap.setCacheIndex(-1);
      file.writePage(oldDeap);
    }
    // else: this heap grows
    else {
      height++;
      file.setCacheSize((maxCacheSize - height) * pageSize);
      if (this.debug) {
        debugFine("NEW CACHESIZE " + (maxCacheSize - height)
                  + " I/O = " + (getPhysicalReadAccess() + getPhysicalWriteAccess()));
      }
    }
    if (this.debug)
      debugFine("***** new cache: " + this);

    // increase deap counter
    numDeaps++;

    return newDeap;
  }

  /**
   * Returns the level of the specified index in this heap.
   *
   * @param index the index for which the level should be returned
   * @return the level of the specified index in this heap
   */
  private int level(int index) {
    return (int) Math.ceil(Math.log(index + 2) / Math.log(2));
  }

  /**
   * Inserts recursively the parent of the specified deap into cache.
   *
   * @param deap the deap for which the parent should be inserted into cache
   */
  private void insertParentToCache(Deap<K, V> deap) {
    if (isRoot(deap))
      return;

    int cacheIndex = deap.getCacheIndex();
    int parentIndex = parentIndex(deap);

    // check, if parent of deap is in cachePath
    Deap<K, V> cacheParent = cachePath[cacheIndex - 1];

    // parent is in cachePath -> ok!
    // else: write cacheParent to disk and insert parent into cachePath
    if (cacheParent.getIndex() != parentIndex) {
      cacheParent.setCacheIndex(-1);
      file.writePage(cacheParent);
      Deap<K, V> parent = file.readPage(parentIndex);
      cachePath[cacheIndex - 1] = parent;
      parent.setCacheIndex(cacheIndex - 1);

      // recursive method call for parent
      insertParentToCache(parent);
    }
  }

  /**
   * Returns true, if the specified deap is the root of this heap, false
   * otherwise.
   *
   * @param deap the deap to be tested for root
   * @return true, if the specified deap is the root of this heap, false
   *         otherwise
   */
  private boolean isRoot(Deap<K,V> deap) {
    return deap.getIndex() == 0;
  }

  /**
   * Returns the index of the parent of the specified deap in this heap.
   *
   * @param deap the deap for which the parent index should be returned
   * @return the index of the parent of the specified deap in this heap
   */
  private int parentIndex(Deap<K,V> deap) {
    return parentIndex(deap.getIndex());
  }

  /**
   * Returns the index of the parent of the deap with the specified index in
   * this heap.
   *
   * @param index the index of the deap for which the parent index should be
   *              returned
   * @return the index of the parent of the deap with the specified index
   */
  private int parentIndex(int index) {
    // node is root
    if (index == 0)
      throw new RuntimeException("Node is root!");

    return (index - 1) / 2;
  }

  /**
   * Returns an arry of the indizes of the deaps in the cache.
   *
   * @return an arry of the indizes of the deaps in the cache
   */
  public Integer[] getCacheIndizes() {
    Integer[] result = new Integer[height];
    for (int i = 0; i < height; i++) {
      result[i] = cachePath[i].getIndex();
    }
    return result;
  }

  /**
   * Returns the parent of the specified deap in the cache.
   *
   * @param deap a deap in this heap
   * @return the parent of the specified deap in the cache
   */
  private Deap<K, V> parentInCache(Deap<K, V> deap) {
    int childIndex = deap.getCacheIndex();
    // deap is root
    if (childIndex == 0)
      throw new IllegalArgumentException("Node is root!");

    return cachePath[childIndex - 1];
  }

  /**
   * Adjusts the first deap recursively with its sons.
   */
  private void adjustFirstDeap() {
    StringBuffer msg = new StringBuffer();

    if (this.debug) {
      msg.append("\n numDeaps = ");
      msg.append(numDeaps);
      msg.append("\n PQ:");
      msg.append(this);
    }

    // get last and first deap
    Deap<K, V> last = getLastDeap();
    Deap<K, V> first = getFirstDeap();

    // only 2 deaps left -> move all from last to first deap,
    // shrink the cache, return
    if (numDeaps == 2) {
      last.moveAll(first);
      shrinkCache();
      return;
    }

    // only 1 deap left -> shrink the cache
    if (numDeaps == 1) {
      shrinkCache();
      return;
    }

    // if last deap is not full -> fill it up with entries from deap before
    if (!last.isFull()) {
      fill(last);
      if (this.debug) {
        msg.append("\n last not full:\n");
        msg.append(this);
      }
    }

    adjust(first, last);
    shrinkCache();

    if (this.debug) {
      msg.append(this);
      debugFine(msg.toString());
    }
  }

  /**
   * Adjusts the entries of the specified parent with its sons.
   *
   * @param parent the parent to be adjusted
   * @param last   the last deap of this heap
   */
  private void adjust(Deap<K, V> parent, Deap<K, V> last) {
    if (isLast(parent))
      return;

    StringBuffer msg = new StringBuffer();
    if (this.debug)
      msg.append(this);

    // parent has children
    if (hasChildren(parent)) {
      Deap<K, V> lson = leftChild(parent);
      Deap<K, V> rson = rightChild(parent);
      Deap<K, V> son, other_son;

      // find the "smaller" son
      if (lson.maxNode().compareTo(rson.maxNode()) < 0) {
        son = lson;
        other_son = rson;
      }
      else {
        son = rson;
        other_son = lson;
      }

      // if son is full -> move all from son to parent
      // heapify parent - other son, heapify parent - last
      if (son.isFull()) {
        son.moveAll(parent);
        heapify(parent, other_son);
        heapify(parent, last);

        // write, if not in cache
        if (!inCache(son))
          file.writePage(son);
        if (!inCache(other_son))
          file.writePage(other_son);
        if (!inCache(parent))
          file.writePage(parent);

        // adjust the son
        adjust(son, last);

        if (this.debug) {
          msg.append("son is full \n");
          msg.append(this);
        }
      }

      // son is not full, i.e. son is next to last parent
      // -> move all from last parent to parent, heapify parent - left
      // son, heapify parent - right son
      else {
        last.moveAll(parent);
        heapify(parent, son);
        heapify(parent, other_son);

        // write, if not in cache
        if (!inCache(son))
          file.writePage(son);
        if (!inCache(other_son))
          file.writePage(other_son);
        if (!inCache(parent))
          file.writePage(parent);

        if (this.debug) {
          msg.append("son is not full \n");
          msg.append(this);
          debugFine(msg.toString());
        }
      }
    }
    // parent has no children, but is not last parent
    else {
      last.moveAll(parent);
      // ausschreiben
      if (!inCache(parent))
        file.writePage(parent);
    }
  }

  /**
   * Shrinks the cache, i.e. removes the last deap from the cache.
   */
  private void shrinkCache() {
    Deap<K, V> last = getLastDeap();

    // decrement number of deaps
    numDeaps--;

    // no more deaps left
    if (numDeaps == 0) {
      cachePath[last.getCacheIndex()] = null;
      last.setCacheIndex(-1);
      file.deletePage(last.getIndex());
      height--;
      return;
    }

    // get the next to last deap and determine its index in cache
    Deap<K, V> newLast = nodeBefore(last);
    int cacheIndex = level(numDeaps - 1) - 1;

    // if newLast.cacheIndex < last.cacheIndex -> this heap decreases
    if (cacheIndex < last.getCacheIndex()) {
      // delete last in cache
      cachePath[last.getCacheIndex()] = null;
      last.setCacheIndex(-1);

      // write old deap
      Deap<K, V> deap = cachePath[cacheIndex];
      deap.setCacheIndex(-1);
      file.writePage(deap);

      // decrease height
      height--;
    }

    // insert new deap into cache
    cachePath[cacheIndex] = newLast;
    newLast.setCacheIndex(cacheIndex);

    // reorganize the cache
    insertParentToCache(newLast);

    // delete (old) last deap from disk
    file.deletePage(last.getIndex());
    if (this.debug)
      debugFine("***** new cache: " + Arrays.asList(getCacheIndizes()));
  }

  /**
   * Returns the deap before the specified deap.
   *
   * @param deap a deap in this heap
   * @return the deap before the specified deap
   */
  private Deap<K, V> nodeBefore(Deap<K, V> deap) {
    if (deap.getCacheIndex() == 0)
      throw new RuntimeException("Node is root!");

    int index = deap.getIndex();
    int beforeIndex = index - 1;

    Deap<K, V> nodeBefore = cachePath[deap.getCacheIndex() - 1];
    if (nodeBefore.getIndex() == beforeIndex)
      return nodeBefore;

    return file.readPage(beforeIndex);
  }

  /**
   * @param deap
   */
  private void fill(Deap<K, V> deap) {
    // get deap next to deap
    Deap<K, V> before = nodeBefore(deap);
    // move from before to deap
    while (!deap.isFull()) {
      deap.addNode(before.getMaxNode());
      // re-establish heaporder
      while (!isRoot(deap)) {
        Deap<K, V> parent = parentInCache(deap);
        HeapNode<K, V> min_deap = deap.minNode();
        HeapNode<K, V> max_parent = parent.maxNode();

        if (max_parent != null && min_deap.compareTo(max_parent) < 0) {
          min_deap = deap.getMinNode();
          max_parent = parent.getMaxNode();
          deap.addNode(max_parent);
          parent.addNode(min_deap);
          deap = parent;
        }
        else
          break;
      }
    }

    // if before is not in cache -> write it to disk
    if (!inCache(before))
      file.writePage(before);
  }

  /**
   * Re-establishes the heaporder: while minimum of the specified deap is
   * greater than maximum of its parent the minimum will be moved up and the
   * maximum will be moved down.
   *
   * @param deap the deap to be tested
   */
  private void heapify(Deap<K, V> deap) {
    if (isRoot(deap))
      return;

    Deap<K, V> parent = parentInCache(deap);
    boolean swap = heapify(parent, deap);

    // recursively method call for parent
    if (swap && !isRoot(parent))
      heapify(parent);
  }

  /**
   * Re-establishes the heaporder: while minimum of the specified max is
   * greater than maximum of the specified min the minimum will be moved up
   * and the maximum will be moved down.
   *
   * @param min the minmum deap to be tested
   * @param max the maximum deap to be tested
   */
  private boolean heapify(Deap<K, V> min, Deap<K, V> max) {
    if (max.minNode() == null)
      return false;

    // if minimum of max is greater than maximum of min ->
    // move up minimum and move down maximum
    boolean swap = false;
    while (min.maxNode().compareTo(max.minNode()) > 0) {
      swap = true;
      HeapNode<K, V> min_node = max.getMinNode();
      HeapNode<K, V> max_node = min.getMaxNode();

      max.addNode(max_node);
      min.addNode(min_node);
    }
    return swap;
  }

  /**
   * Returns true if the specified deap is in the cache, false otherwise.
   *
   * @param deap the deap to be tested
   * @return true if the specified deap is in the cache, false otherwise
   */
  private boolean inCache(Deap<K,V> deap) {
    return deap.getCacheIndex() >= 0;
  }

  /**
   * Returns true if the deap with the specified index is in the cache, false
   * otherwise.
   *
   * @param index the index of the deap to be tested
   * @return true if the deap with the specified index is in the cache, false
   *         otherwise
   */
  private boolean inCache(int index) {
    int cacheIndex = cacheIndex(index);
    return cachePath[cacheIndex].getIndex() == index;

    // for (int i = 0; i < maxCacheSize; i++) {
    // Deap deap = cachePath[i];
    // if (deap == null)
    // return false;
    // if (deap.getIndex() == index)
    // return true;
    // if (deap.getIndex() > index)
    // return false;
    // }
    // return false;
  }

  /**
   * Returns the cache index of the specified index if the index would be in
   * cache.
   *
   * @param index the index of the element
   * @return the cache index
   */
  private int cacheIndex(int index) {
    double log = Math.log(index + 1) / Math.log(2);
    return (int) Math.floor(log + 1) - 1;
  }

  /**
   * Returns the deap with the specified index. If the deap is not in cache it
   * will be read from file.
   *
   * @param index the index of the deap to be returned
   * @return the deap with the specified index
   */
  private Deap<K, V> getDeap(int index) {
    int cacheIndex = cacheIndex(index);
    Deap<K, V> deapInCache = cachePath[cacheIndex];

    if (deapInCache.getIndex() == index)
      return deapInCache;
    else
      return file.readPage(index);
  }

  /**
   * Returns true if the specified deap is the last deap of this heap, false
   * otherwise.
   *
   * @param deap the deap to be tested
   * @return true if the specified deap is the last deap of this heap, false
   *         otherwise
   */
  private boolean isLast(Deap<K,V> deap) {
    return deap.getIndex() == getLastDeap().getIndex();
  }

  /**
   * Returns true if the specified deap has children, false otherwise.
   *
   * @param deap the deap to be tested
   * @return true if the specified deap has children, false otherwise
   */
  private boolean hasChildren(Deap<K,V> deap) {
    int maxParentIndex = parentIndex(numDeaps);
    return deap.getIndex() < maxParentIndex;
  }

  /**
   * Returns the left child of the specified deap in this heap.
   *
   * @param deap a deap in this heap
   * @return the left child of the specified deap in this heap
   */
  private Deap<K, V> leftChild(Deap<K, V> deap) {
    if (deap.getCacheIndex() >= maxCacheSize)
      throw new IllegalArgumentException("Node has no children!");

    int parentIndex = deap.getIndex();
    int leftChildIndex = 2 * parentIndex + 1;

    Deap<K, V> child = cachePath[deap.getCacheIndex() + 1];
    if (child.getIndex() == leftChildIndex)
      return child;

    return file.readPage(leftChildIndex);
  }

  /**
   * Returns the right child of the specified deap in this heap.
   *
   * @param deap a deap in this heap
   * @return the right child of the specified deap in this heap
   */
  private Deap<K, V> rightChild(Deap<K,V> deap) {
    if (deap.getCacheIndex() >= maxCacheSize)
      throw new RuntimeException("Node has no children!");

    int parentIndex = deap.getIndex();
    int rightChildIndex = 2 * parentIndex + 2;

    Deap<K, V> child = cachePath[deap.getCacheIndex() + 1];
    if (child.getIndex() == rightChildIndex)
      return child;

    return file.readPage(rightChildIndex);
  }

  public static void main(String[] args) {
    PersistentHeap<Integer, DefaultIdentifiable> heap1 = new PersistentHeap<Integer, DefaultIdentifiable>(
        4000, 80000, 8);
    DefaultHeap<Integer, DefaultIdentifiable> heap2 = new DefaultHeap<Integer, DefaultIdentifiable>();
    // Deap<Integer, DefaultIdentifiable> heap1 = new Deap<Integer,
    // DefaultIdentifiable>(50, -1, -1);

    Random random = new Random(210571);

    for (int i = 0; i < 100000; i++) {
      int key = random.nextInt(1000);
      heap1.addNode(new DefaultHeapNode<Integer, DefaultIdentifiable>(
          key, new DefaultIdentifiable(i)));
      heap2.addNode(new DefaultHeapNode<Integer, DefaultIdentifiable>(
          key, new DefaultIdentifiable(i)));
    }

    for (int i = 0; i < 100000; i++) {
      HeapNode<Integer, DefaultIdentifiable> n1 = heap1.getMinNode();
      HeapNode<Integer, DefaultIdentifiable> n2 = heap2.getMinNode();

      if (!n1.getKey().equals(n2.getKey())) {
        System.out.println("i " + i);
        System.out.println("key n1.key != n2.key " + n1 + " != " + n2);
        System.out.println(heap1);
        throw new RuntimeException();
      }
      if (!n1.getValue().equals(n2.getValue())) {
        System.out.println("i " + i);
        System.out.println("n1.value != n2.value " + n1 + " != " + n2);
        System.out.println(heap2);
        throw new RuntimeException();
      }
    }
  }
}
