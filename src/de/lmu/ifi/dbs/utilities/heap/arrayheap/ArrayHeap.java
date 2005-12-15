package de.lmu.ifi.dbs.utilities.heap.arrayheap;

import de.lmu.ifi.dbs.persistent.LRUCache;
import de.lmu.ifi.dbs.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.heap.arrayheap.SlotPage;
import de.lmu.ifi.dbs.utilities.heap.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ArrayHeap<K extends Comparable<K>, V extends Identifiable> implements Heap<K, V> {
  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The debugging flag.
   */
  private static boolean DEBUG = true;

  /**
   * Maximum number of levels in the external collection
   */
  private final static int L = 4;

  /**
   * The constant c according to the Array Heap paper.
   */
  private final double c = 1.0 / 7.0;

  /**
   * Indicates weather this heap is organised in ascending or descending order.
   */
  private boolean ascending;

  /**
   * The constant M according to the Array heap paper.
   */
  private final double M;

  /**
   * The maximum number of elements within one page.
   */
  private final int B;

  /**
   * The number of slots in each level.
   */
  private final int mu;

  /**
   * The size of a node (i.e. an element in the heap) in Bytes.
   */
  private final int nodeSize;

  /**
   * The maximum size of the h1 heap.
   */
  private final int maxSize_h1;

  /**
   * Internal heap that stores the newly insertes elements.
   */
  private DefaultHeap<K, V> h1;

  /**
   * Internal heap that stores at most one page of the smallest elements of each non-empty slot.
   */
  private SlotPage<K, V>[] h2;

  /**
   * The page file storing the pages of the slots in the external levels.
   */
  private final PageFile<SlotPage<K, V>> file;

  /**
   * Creates a new heap that stores the elements in ascending order.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes (i.e. the maximum number of Bytes hold in
   *                  internal memory)
   * @param nodeSize  the size of a node (i.e. an element in the heap) in Bytes
   */
  public ArrayHeap(int pageSize, int cacheSize, int nodeSize) {
    this(true, pageSize, cacheSize, nodeSize);
  }

  /**
   * Creates a new heap that stores the elements in the specified order.
   *
   * @param ascending if true, the heap is organised in ascending order, otherwise
   *                  the heap is organised in descending other
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes (i.e. the maximum number of Bytes hold in
   *                  internal memory)
   * @param nodeSize  the size of a node (i.e. an element in the heap) in Bytes
   */
  public ArrayHeap(boolean ascending, int pageSize, int cacheSize, int nodeSize) {
    initLogger();
    this.ascending = ascending;
    this.nodeSize = nodeSize;
    this.file = new MemoryPageFile<SlotPage<K, V>>(pageSize, 1, new LRUCache<SlotPage<K, V>>());

    this.M = (cacheSize + L * pageSize) / ((2 + L) * c);

    // number of slots in each level
    this.mu = (int) (c * M / pageSize - 1);
    if (this.mu < 1) throw new IllegalArgumentException("Cache size of " + cacheSize +
                                                        " Bytes is chosen too small for a page size of " +
                                                        pageSize + " Bytes!");

    // max. number of elements within a page
    this.B = (int) ((pageSize * 1.0) / (nodeSize * 1.0));
    this.maxSize_h1 = (int) ((2 * c * M) / nodeSize);
    int maxSize_h2 = (int) ((c * M / pageSize - 1) * L);

    this.h1 = new DefaultHeap<K, V>(ascending);
    //noinspection unchecked
    this.h2 = new SlotPage[maxSize_h2];

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   pageSize ").append(pageSize);
      msg.append("\n   cacheSize ").append(cacheSize);
      msg.append("\n   nodeSize ").append(nodeSize);
      msg.append("\n\n   L ").append(L);
      msg.append("\n   c ").append(c);
      msg.append("\n   M ").append(M);
      msg.append("\n   B ").append(B);
      msg.append("\n   mu ").append(mu);
      msg.append("\n   maxSize_h1 obj ").append(maxSize_h1);
      msg.append("\n   maxSize_h2 obj ").append(maxSize_h2 * B);
      msg.append("\n   maxSize_h2 pages ").append(maxSize_h2);
      logger.info(msg.toString());
      System.out.println(msg);
    }

  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public void addNode(HeapNode<K, V> node) {
    if (h1.size() < maxSize_h1) {
      h1.addNode(node);
      return;
    }

    StringBuffer msg = new StringBuffer();
    int n = (int) (c * M / nodeSize);
    if (DEBUG) msg.append("\n   c * M = " + n);

    List<HeapNode<K, V>> s = new ArrayList<HeapNode<K, V>>();
    for (int i = 0; i < n; i++) {
      s.add(h1.getMinNode());
    }
    if (DEBUG) msg.append("\n   s = " + s);

    for (int i = 1; i <= L; i++) {
      

      for (int j = 1; j <= mu; j++) {
        int pageID = getFirstPageID(i, j);
        SlotPage<K, V> page = file.readPage(pageID);
        if (page == null) {
          page = new SlotPage<K, V>(B);
          file.writePage(page);
        }
        if (page.isEmpty()) {
          h2[((i - 1) * mu + j - 1)] = page;
          store(s, page);
          System.out.println(this.toString());
          return;
        }


      }
    }

    if (DEBUG) logger.info(msg.toString());
    throw new UnsupportedOperationException();
  }

  private void store(List<HeapNode<K, V>> s, SlotPage<K, V> page) {
    StringBuffer msg = new StringBuffer();
    if (DEBUG) msg.append("\n   store " + s.size() + " objects in page " + page);

    int length = s.size() < B ? s.size() : B;

    Iterator<HeapNode<K,V>> it = s.iterator();
    for (int i = 0; i < length; i++) {
      page.insertNode(it.next());
      it.remove();
    }

    if (DEBUG) logger.info(msg.toString());

    if (! s.isEmpty()) {
      page = file.readPage(page.getID() + 1);
      if (page == null) {
        page = new SlotPage<K, V>(B);
        file.writePage(page);
      }
      store(s, page);
    }

  }

  private int getFirstPageID(int level, int slot) {
    if (level < 1)
      throw new IllegalArgumentException("Level must be >= 1!");
    if (slot < 1)
      throw new IllegalArgumentException("Slot must be >= 1!");

    int pageNumber = 0;

    for (int i = 1; i <= level; i++) {
      // length of the slots in level i
      int l_i = (int) (Math.pow(c * M / nodeSize, i) / Math.pow(B, i - 1));

      // number of pages in all slots in level i
      if (i < level) {
        pageNumber += mu * l_i / B;
      }
      // number of pages from slot_1 to the specified slot in level i
      else {
        pageNumber += (slot - 1) * l_i / B;
      }
    }

    return pageNumber;
  }

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public HeapNode<K, V> getMinNode() {
    throw new UnsupportedOperationException();
  }

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  public boolean isEmpty() {
    throw new UnsupportedOperationException();
  }

  /**
   * Clears this heap.
   */
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the current index of the specified value in this heap.
   *
   * @param value the value for which the index should be returned
   * @return the current index of the specified value in this heap
   */
  public Integer getIndexOf(V value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the node at the specified index.
   *
   * @param index the index of the node to be returned
   * @return the node at the specified index
   */
  public HeapNode<K, V> getNodeAt(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Moves up a node at the specified index until it satisfies the heaporder.
   *
   * @param index the index of the node to be moved up.
   */
  public void flowUp(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the size of this heap.
   *
   * @return the size of this heap
   */
  public int size() {
    throw new UnsupportedOperationException();
  }


  public static void main(String[] args) {
    ArrayHeap<Integer, Identifiable> heap = new ArrayHeap<Integer, Identifiable>(4000, 80000, 8);
    Random random = new Random(210571);

    for (int i = 0; i < 20000; i++) {
      int id = random.nextInt(10000);
      heap.addNode(new DefaultHeapNode<Integer, Identifiable>(id, new DefaultIdentifiable(id)));
    }
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(ArrayHeap.class.toString());
    logger.setLevel(Level.OFF);
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("\nH1 " + h1);
    result.append("\nH2 " + Arrays.asList(h2));

    for (int i = 1; i <= L; i++) {
      result.append("\nLevel " + i);
      for (int j = 1; j <= mu; j++) {
        int l_i = (int) (Math.pow(c * M / nodeSize, i) / Math.pow(B, i - 1));
        int no_pages_i = l_i / B;
        int pageID = getFirstPageID(i, j);
        result.append("\n   slot " + j + ": ");
        for (int p = 0; p < no_pages_i; p++) {
          SlotPage<K,V> page = file.readPage(p + pageID);
          if (page != null)
          result.append((p + pageID) + " ");
        }
      }
    }

    result.append("\nI/O " + file.getIOAccess());
    return result.toString();
  }


}
