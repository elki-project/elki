package de.lmu.ifi.dbs.utilities.heap;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistent implementation of a heap-based priority queue.
 * The heap is organised according to the natural order of the stored objects.
 * Elements stored in this heap must be instances of  <code>PersistentHeapNode<\code>
 * (@see PersistentHeapNode).
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PersistentHeap implements Heap {
  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  private static Level level = Level.ALL;

  /**
   * The file storing the elements of this heap.
   */
  private final PersistentHeapFile file;

  /**
   * The number of elements in this heap.
   */
  private int numElements;

  /**
   * The maximum height of this heap.
   */
  private final int maxHeight;

  /**
   * The maximum index of a deap in this heap.
   */
  private final int maxDeapIndex;

  /**
   * The maximum length of a deap.
   */
  private final int maxDeapLength;

  /**
   * The actual height of this heap.
   */
  private int height;

  /**
   * The actual number of Deaps in this heap.
   */
  private int numDeaps;

  /**
   * The actual path of this heap im main memory. This array consists of one path from root to
   * the last leaf.
   */
  private Deap[] cachePath;

  /**
   * Creates a new persistent heap with the specified parameters.
   *
   * @param fileName  the name of the file storing the heap
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param node      the node to be added to this newly created heap (must be not null for
   *                  initialization purposes)
   */
  public PersistentHeap(String fileName, int pageSize, int cacheSize,
                        PersistentHeapNode node) {
    initLogger();
    StringBuffer msg = new StringBuffer();

    this.file = new PersistentHeapFile(fileName);
    this.numElements = 0;
    this.maxHeight = cacheSize / pageSize;
    this.cachePath = new Deap[maxHeight];
    this.maxDeapIndex = (int) (Math.pow(2, maxHeight) - 2);
    this.maxDeapLength = (int) (pageSize / node.size());
    this.height = 0;
    this.numDeaps = 0;

    msg.append("\n pageSize = ");
    msg.append(pageSize);
    msg.append("\n maxHeight = ");
    msg.append(maxHeight);
    msg.append("\n maxDeapIndex = ");
    msg.append(maxDeapIndex);
    msg.append("\n maxDeapLength = ");
    msg.append(maxDeapLength);
    logger.info(msg.toString());

    addNode(node);
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public synchronized void addNode(final HeapNode node) {
    if (! (node instanceof PersistentHeapNode))
      throw new IllegalArgumentException("Node has to be instance of PersistentHeapNode!");

    StringBuffer msg = new StringBuffer();

    // get last deap in cachePath
    Deap deap = getLastDeap();

    // cachePath is empty at first
    if (deap == null) {
      msg.append("Cache is empty, create new deap!");
      deap = createNewLastDeap();
    }

    // last deap is full
    else if (deap.isFull()) {
      // no more deap fits into cache
      if (deap.getIndex() == maxDeapIndex)
        throw new IllegalArgumentException("Cache is full!");

      // else: create new deap and reorganize cache
      msg.append("Last deap is full, create new deap!");
      deap = createNewLastDeap();
    }

    // insert node in last deap
    deap.addNode(node);

    // re-establish heap order
    heapify(deap);

    numElements++;

    msg.append("\n add ");
    msg.append(node);
    msg.append("\n");
    msg.append(this);
    logger.info(msg.toString());
  }

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public HeapNode getMinNode() {
    if (numElements < 1)
      throw new RuntimeException("No elements in priority queue!");

    StringBuffer msg = new StringBuffer();

    // get first deap
    Deap deap = getFirstDeap();
    // get first element
    PersistentHeapNode min = (PersistentHeapNode) deap.getMinNode();

    // if first deap is empty, adjust recursively with sons
    if (deap.isEmpty()) {
      msg.append("First deap is empty --> adjust it!");
      adjustFirstDeap();
    }

    numElements--;

    msg.append("\n add ");
    msg.append(min);
    msg.append("\n");
    msg.append(this);
    logger.info(msg.toString());

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
   * Clears this heap.
   */
  public void clear() {
    this.file.clear();
    this.numElements = 0;
    this.cachePath = new Deap[maxHeight];
    this.height = 0;
    this.numDeaps = 0;
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
        Deap deap = cachePath[pathIndex++];

        buffer.append(deap);
        buffer.append("*(");
        buffer.append(deap.getCacheIndex());
        buffer.append(") --- ");

        if (deap.getCacheIndex() == -1) {
          System.out.println(buffer.toString());
          throw new RuntimeException();
        }
      }
      else {
        Deap deap = file.read(i);

        buffer.append(deap);
        buffer.append("(");
        buffer.append(deap.getCacheIndex());
        buffer.append(") --- ");

        if (deap.getCacheIndex() != -1) {
          System.out.println(buffer.toString());
          throw new RuntimeException();
        }
      }
    buffer.append("\n\n");
    return buffer.toString();

  }

  /**
   * Returns the last deap in the cache path.
   *
   * @return the last deap in the cache path
   */
  private Deap getLastDeap() {
    if (height == 0) {
      return null;
    }
    logger.info("height = " + height);
    return cachePath[height - 1];
  }

  /**
   * Returns the first deap in the cache path.
   *
   * @return the first deap in the cache path
   */
  private Deap getFirstDeap() {
    return cachePath[0];
  }

  /**
   * Creates and returns a new deap as last element of the cache path.
   *
   * @return a new deap as last element of the cache path
   */
  private Deap createNewLastDeap() {
    // determine index of the new deap in the cachePath
    int cacheIndex = level(numDeaps) - 1;

    // create new deap
    Deap newDeap = new Deap(maxDeapLength, numDeaps, cacheIndex);

    // get old deap from cachePath
    Deap oldDeap = cachePath[cacheIndex];

    // insert the new deap into cache
    cachePath[cacheIndex] = newDeap;

    // reorganize th cache: insert parents of newDeap into cachePath
    insertParentToCache(newDeap);

    // oldDeap has been replaced in cache
    if (oldDeap != null) {
      // set cacheIndex of oldDeap to -1 and write oldDeap to disk
      oldDeap.setCacheIndex(-1);
      file.write(oldDeap);
    }
    // else: this heap grows
    else {
      height++;
    }
    logger.info("***** new cache: " + Arrays.asList(getCacheIndizes()));

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
  private void insertParentToCache(Deap deap) {
    if (isRoot(deap)) return;

    int cacheIndex = deap.getCacheIndex();
    int parentIndex = parentIndex(deap);

    // check, if parent of deap is in cachePath
    Deap cacheParent = cachePath[cacheIndex - 1];

    // parent is in cachePath -> ok!
    // else: write cacheParent to disk and insert parent into cachePath
    if (cacheParent.getIndex() != parentIndex) {
      cacheParent.setCacheIndex(-1);
      file.write(cacheParent);
      Deap parent = file.read(parentIndex);
      cachePath[cacheIndex - 1] = parent;
      parent.setCacheIndex(cacheIndex - 1);

      // recursive method call for parent
      insertParentToCache(parent);
    }
  }

  /**
   * Returns true, if the specified deap is the root of this heap, false otherwise.
   *
   * @param deap the deap to be tested for root
   * @return true, if the specified deap is the root of this heap, false otherwise
   */
  private boolean isRoot(Deap deap) {
    return deap.getIndex() == 0;
  }

  /**
   * Returns the index of the parent of the specified deap in this heap.
   *
   * @param deap the deap for which the parent index should be returned
   * @return the index of the parent of the specified deap in this heap
   */
  private int parentIndex(Deap deap) {
    return parentIndex(deap.getIndex());
  }

  /**
   * Returns the index of the parent of the deap with the specified index in this heap.
   *
   * @param index the index of the deap for which the parent index should be returned
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
  private Deap parentInCache(Deap deap) {
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

    msg.append("\n numDeaps = ");
    msg.append(numDeaps);
    msg.append("\n PQ:");
    msg.append(this);

    // get last and first deap
    Deap last = getLastDeap();
    Deap first = getFirstDeap();

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
      msg.append("\n last not full:\n");
      msg.append(this);
    }

    adjust(first, last);
    shrinkCache();

    msg.append(this);
    logger.info(msg.toString());
  }

  /**
   * Adjusts the entries of the specified parent with its sons.
   *
   * @param parent the parent to be adjusted
   * @param last the last deap of this heap
   */
  private void adjust(Deap parent, Deap last) {
    if (isLast(parent)) return;

    StringBuffer msg = new StringBuffer();
    msg.append(this);

    // parent has children
    if (hasChildren(parent)) {
      Deap lson = leftChild(parent);
      Deap rson = rightChild(parent);
      Deap son, other_son;

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
      // heapify parent  - other son, heapify parent - last
      if (son.isFull()) {
        son.moveAll(parent);
        heapify(parent, other_son);
        heapify(parent, last);

        // write, if not in cache
        if (!inCache(son))
          file.write(son);
        if (!inCache(other_son))
          file.write(other_son);
        if (!inCache(parent))
          file.write(parent);

        // adjust the son
        adjust(son, last);

        msg.append("son is full \n");
        msg.append(this);
      }

      // son is not full, i.e. son is next to last parent
      // -> move all from last parent to parent, heapify parent - left son, heapify parent - right son
      else {
        last.moveAll(parent);
        heapify(parent, son);
        heapify(parent, other_son);

        // write, if not in cache
        if (!inCache(son))
          file.write(son);
        if (!inCache(other_son))
          file.write(other_son);
        if (!inCache(parent))
          file.write(parent);

        msg.append("son is not full \n");
        msg.append(this);
      }
    }
    // parent has no children, but is not last parent
    else {
      last.moveAll(parent);
      // ausschreiben
      if (!inCache(parent))
        file.write(parent);
    }
  }

  /**
   * Shrinks the cache, i.e. removes the last deap from the cache.
   */
  private void shrinkCache() {
    Deap last = getLastDeap();

    // decrement number of deaps
    numDeaps--;

    // no more deaps left
    if (numDeaps == 0) {
      cachePath[last.getCacheIndex()] = null;
      last.setCacheIndex(-1);
      file.delete(last.getIndex());
      height--;
      return;
    }

    // get the next to last deap and determine its index in cache
    Deap newLast = nodeBefore(last);
    int cacheIndex = level(numDeaps - 1) - 1;

    // if newLast.cacheIndex < last.cacheIndex -> this heap decreases
    if (cacheIndex < last.getCacheIndex()) {
      // delete last in cache
      cachePath[last.getCacheIndex()] = null;
      last.setCacheIndex(-1);

      // write old deap
      Deap deap = cachePath[cacheIndex];
      deap.setCacheIndex(-1);
      file.write(deap);

      // decrease height
      height--;
    }

    // insert new deap into cache
    cachePath[cacheIndex] = newLast;
    newLast.setCacheIndex(cacheIndex);

    // reorganize the cache
    insertParentToCache(newLast);

    // delete (old) last deap from disk
    file.delete(last.getIndex());
    logger.info("***** new cache: " + Arrays.asList(getCacheIndizes()));
  }

  /**
   * Returns the deap before the specified deap.
   *
   * @param deap a deap in this heap
   * @return the deap before the specified deap
   */
  private Deap nodeBefore(Deap deap) {
    if (deap.getCacheIndex() == 0)
      throw new RuntimeException("Node is root!");

    int index = deap.getIndex();
    int beforeIndex = index - 1;

    Deap nodeBefore = cachePath[deap.getCacheIndex() - 1];
    if (nodeBefore.getIndex() == beforeIndex)
      return nodeBefore;

    return file.read(beforeIndex);
  }

  /**
   * @param deap
   */
  private void fill(Deap deap) {
    // get deap next to deap
    Deap before = nodeBefore(deap);
    // move from before to deap
    while (!deap.isFull()) {
      deap.addNode(before.getMaxNode());
      // re-establish heaporder
      while (!isRoot(deap)) {
        Deap parent = parentInCache(deap);
        HeapNode min_deap = deap.minNode();
        HeapNode max_parent = parent.maxNode();

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
      file.write(before);
  }

  /**
   * Re-establishes the heaporder: while minimum of the specified deap
   * is greater than maximum of its parent the minimum will be moved up
   * and the maximum will be moved down.
   *
   * @param deap the deap to be tested
   */
  private void heapify(Deap deap) {
    if (isRoot(deap)) return;

    Deap parent = parentInCache(deap);
    boolean swap = heapify(deap, parent);

    // recursively method call for parent
    if (swap && ! isRoot(parent)) heapify(parent, parentInCache(parent));
  }

  /**
   * Re-establishes the heaporder: while minimum of the specified max
   * is greater than maximum of the specified min the minimum will be moved up
   * and the maximum will be moved down.
   *
   * @param min the minmum deap to be tested
   * @param max the maximum deap to be tested
   */
  private boolean heapify(Deap min, Deap max) {
    HeapNode min_deap = max.minNode();
    if (min_deap == null) return false;
    HeapNode max_deap = min.maxNode();

    // if minimum of max is greater than maximum of min ->
    // move up minimum and move down maximum
    boolean swap = false;
    while (min_deap.compareTo(max_deap) < 0) {
      System.out.println("this " + this);
      swap = true;
      min_deap = max.getMinNode();
      max_deap = min.getMaxNode();

      max.addNode(max_deap);
      min.addNode(min_deap);
    }
    return swap;
  }

  /**
   * Returns true if the specified deap is in the cache, false otherwise.
   *
   * @param deap the deap to be tested
   * @return true if the specified deap is in the cache, false otherwise
   */
  private boolean inCache(Deap deap) {
    return deap.getCacheIndex() >= 0;
  }

  /**
   * Returns true if the deap with the specified index is in the cache, false otherwise.
   *
   * @param index the index of the deap to be tested
   * @return true if the deap with the specified index is in the cache, false otherwise
   */
  private boolean inCache(int index) {
    for (int i = 0; i < maxHeight; i++) {
      Deap deap = cachePath[i];
      if (deap == null)
        return false;
      if (deap.getIndex() == index)
        return true;
      if (deap.getIndex() > index)
        return false;
    }
    return false;
  }

  /**
   * Returns true if the specified deap is the last deap of this heap, false otherwise.
   *
   * @param deap the deap to be tested
   * @return true if the specified deap is the last deap of this heap, false otherwise
   */
  private boolean isLast(Deap deap) {
    return deap.getIndex() == getLastDeap().getIndex();
  }

  /**
   * Returns true if the specified deap has children, false otherwise.
   *
   * @param deap the deap to be tested
   * @return true if the specified deap has children, false otherwise
   */
  private boolean hasChildren(Deap deap) {
    int maxParentIndex = parentIndex(numDeaps);
    return deap.getIndex() < maxParentIndex;
  }

  /**
   * Returns the left child of the specified deap in this heap.
   *
   * @param deap a deap in this heap
   * @return the left child of the specified deap in this heap
   */
  private Deap leftChild(Deap deap) {
    if (deap.getCacheIndex() >= maxHeight)
      throw new IllegalArgumentException("Node has no children!");

    int parentIndex = deap.getIndex();
    int leftChildIndex = 2 * parentIndex + 1;

    Deap child = cachePath[deap.getCacheIndex() + 1];
    if (child.getIndex() == leftChildIndex)
      return child;

    return file.read(leftChildIndex);
  }

  /**
   * Returns the right child of the specified deap in this heap.
   *
   * @param deap a deap in this heap
   * @return the right child of the specified deap in this heap
   */
  private Deap rightChild(Deap deap) {
    if (deap.getCacheIndex() >= maxHeight)
      throw new RuntimeException("Node has no children!");

    int parentIndex = deap.getIndex();
    int rightChildIndex = 2 * parentIndex + 2;

    Deap child = cachePath[deap.getCacheIndex() + 1];
    if (child.getIndex() == rightChildIndex)
      return child;

    return file.read(rightChildIndex);
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(PersistentHeap.class.toString());
    logger.setLevel(level);
  }

  public static void main(String[] args) {
    PersistentHeap pq = new PersistentHeap("pqtest", 60, 60 * 3, new DefaultPersistentHeapNode(5, new DefaultPersistentKey(5)));

    pq.addNode(new DefaultPersistentHeapNode(7, new DefaultPersistentKey(7)));
//    System.out.println(7);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(2, new DefaultPersistentKey(2)));
//    System.out.println(2);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(9, new DefaultPersistentKey(9)));
//    System.out.println(9);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(6, new DefaultPersistentKey(6)));
//    System.out.println(6);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(3, new DefaultPersistentKey(3)));
//    System.out.println(3);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(8, new DefaultPersistentKey(8)));
//    System.out.println(8);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(12, new DefaultPersistentKey(12)));
//    System.out.println(12);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(10, new DefaultPersistentKey(10)));
//    System.out.println(10);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(14, new DefaultPersistentKey(14)));
//    System.out.println(14);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(11, new DefaultPersistentKey(11)));
//    System.out.println(11);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(1, new DefaultPersistentKey(1)));
//    System.out.println(1);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(18, new DefaultPersistentKey(18)));
//    System.out.println(18);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(4, new DefaultPersistentKey(4)));
//    System.out.println(4);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(21, new DefaultPersistentKey(21)));
//    System.out.println(21);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(28, new DefaultPersistentKey(28)));
//    System.out.println(28);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(-3, new DefaultPersistentKey(-3)));
//    System.out.println(-3);
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(65, new DefaultPersistentKey(65)));
//    System.out.println("add 65");
//    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(-5, new DefaultPersistentKey(-5)));
    System.out.println("add -5");
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    System.out.println("\n" + pq.getMinNode());
    System.out.println(pq);

    pq.addNode(new DefaultPersistentHeapNode(-5, new DefaultPersistentKey(-5)));
    System.out.println("add -5");
    System.out.println(pq);

  }
}
