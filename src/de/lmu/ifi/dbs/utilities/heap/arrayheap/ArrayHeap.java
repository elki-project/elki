package de.lmu.ifi.dbs.utilities.heap.arrayheap;

import de.lmu.ifi.dbs.persistent.LRUCache;
import de.lmu.ifi.dbs.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.heap.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
   * First ID that can be used for temporal pages.
   */
  private final int firstTempPageID;

  /**
   * The size of this heap.
   */
  private int size;

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
    int maxSize_h2 = mu * L;
    maxSize_h1 = (cacheSize / pageSize - maxSize_h2) * B;

    this.h1 = new DefaultHeap<K, V>(ascending);
    //noinspection unchecked
    this.h2 = new SlotPage[maxSize_h2];

    // max page ID
    firstTempPageID = getFirstPageID(L + 1, 1);

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   pageSize ").append(pageSize);
      msg.append("\n   cacheSize bytes ").append(cacheSize);
      msg.append("\n   cacheSize pages ").append(cacheSize / pageSize);
      msg.append("\n   nodeSize ").append(nodeSize);
      msg.append("\n\n   L ").append(L);
      msg.append("\n   c ").append(c);
      msg.append("\n   M ").append(M);
      msg.append("\n   B ").append(B);
      msg.append("\n   mu ").append(mu);
      msg.append("\n   maxSize_h1 obj ").append(maxSize_h1);
      msg.append("\n   maxSize_h1 pages ").append(maxSize_h1 / B);
      msg.append("\n   maxSize_h2 obj ").append(maxSize_h2 * B);
      msg.append("\n   maxSize_h2 pages ").append(maxSize_h2);
      msg.append("\n   firstTempPageID ").append(firstTempPageID);
      msg.append("\n   max number of objects ").append(firstTempPageID * B + maxSize_h1);
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
    // todo indices
    if (h1.size() < maxSize_h1) {
      h1.addNode(node);
      size++;
      return;
    }

    System.out.println(this);
    StringBuffer msg = new StringBuffer();
    List<Integer> s = new ArrayList<Integer>();
    for (int i = 0; i < L; i++) {
      if (DEBUG) {
        msg.append("\nmerge level " + i + " with " + s);
        System.out.println("\nmerge level " + i + " with " + s);
      }
      s = mergeLevel(i, s);

      for (int j = 1; j <= mu; j++) {
        int firstID = getFirstPageID(i + 1, j);
        SlotPage<K, V> firstPage = getPage(firstID + 1);
        if (firstPage.isEmpty()) {
          if (DEBUG) {
            msg.append("FIRST PAGE " + firstPage + " EMPTY");
            System.out.println("FIRST PAGE " + firstPage + " EMPTY");
          }

          int tmpID = s.remove(0);
          SlotPage<K, V> h2Page = file.readPage(tmpID);
          h2Page.setID(firstID);
          file.deletePage(tmpID);
          h2[(i * mu + j - 1)] = h2Page;
          System.out.println("h2[" + (i * mu + j - 1) + "] = " + h2Page);

          store(s, firstPage);
          System.out.println(this.toString());
          System.out.println("*************************************");
          size++;
          h1.addNode(node);
          return;
        }
        else System.out.println("FIRST PAGE " + firstPage + " NOT EMPTY");
      }
    }

    if (DEBUG) {
      logger.info(msg.toString());
    }
    throw new IllegalStateException("Queue is full: " + size + " objects!");
  }

  private List<Integer> mergeLevel(int level, List<Integer> s) {
    StringBuffer msg = new StringBuffer();

    // number of pages needed for merging
    int numberOfPages = (int) (Math.pow(mu + 1, level) * (this.maxSize_h1 / 2 / B));
    if (DEBUG) {
      msg.append("   number of pages needed for merging = " + numberOfPages);
      System.out.println(msg);
    }

    List<Integer> result = new ArrayList<Integer>(numberOfPages);

    // first level
    if (level == 0) {
      for (int p = 0; p < numberOfPages; p++) {
        SlotPage<K, V> page = new SlotPage<K, V>(B);
        page.setID(firstTempPageID + p);
        for (int n = 0; n < B; n++) {
          page.insertNode(h1.getMinNode());
        }
        int tempID = file.writePage(page);
        result.add(tempID);
      }
    }

    // all other levels
    else {
      int numberOfPagesInLowerLevel = (int) (Math.pow(mu + 1, level - 1) * (this.maxSize_h1 / 2 / B));
      int tmpOffset = s.get(s.size() - 1) - firstTempPageID + 1;
      int currentSlot = 1;
      int currentPageOffset = 0;

      SlotPage<K, V> page1 = file.readPage(s.get(0));
      SlotPage<K, V> page2 = h2[(level - 1) * mu];
      if (DEBUG) {
//        System.out.println("xxx page1 " + page1 + " -> " + page1.getNodes().size());
//        System.out.println("xxx page2 " + page2 + " -> " + page2.getNodes().size());
      }

      for (int p = 0; p < numberOfPages; p++) {
        SlotPage<K, V> tmpPage = new SlotPage<K, V>(B);
        tmpPage.setID(firstTempPageID + p + tmpOffset);

        for (int n = 0; n < B; n++) {
          HeapNode<K, V> n1 = page1 == null ? null : page1.getNodes().get(0);
          HeapNode<K, V> n2 = page2 == null ? null : page2.getNodes().get(0);

          if (n1 == null && n2 == null) {
            System.out.println(result);
            throw new RuntimeException("Should never happen! p=" + p + " n= " + n);
          }

          if (n2 == null || n1 != null && n1.compareTo(n2) <= 0) {
            page1.getNodes().remove(0);
            if (page1.getNodes().isEmpty()) {
              file.deletePage(page1.getID());
              s.remove(0);
              page1 = s.isEmpty() ? null : file.readPage(s.get(0));
//              if (page1 != null) System.out.println("   page1 " + page1 + " -> " + page1.getNodes().size());
            }
            tmpPage.insertNode(n1);
          }
          if (n1 == null || n2 != null && n1.compareTo(n2) > 0) {
            page2.getNodes().remove(0);
            if (page2.getNodes().isEmpty()) {
//              System.out.println("   page2.getNodes().isEmpty() " + page2 + " currentPageOffset " + currentPageOffset);
              // zähler erhöhen
              currentPageOffset++;
              if (currentPageOffset == numberOfPagesInLowerLevel) {
                currentPageOffset = 0;
                currentSlot++;

                if (currentSlot > mu) {
                  page2 = null;
                }
                else {
                  page2 = h2[(level - 1) * mu + currentSlot - 1];
//                  System.out.println("h2[" + ((level - 1) * mu + currentSlot - 1) + "] = " + page2);
                }
              }

              else {
                int pageID = getFirstPageID(level, currentSlot) + currentPageOffset;
                page2 = file.readPage(pageID);
              }
//              if (page2 != null) System.out.println("   page2 " + page2 + " -> " + page2.getNodes().size());
            }
            tmpPage.insertNode(n2);
          }
        }
        int tempID = file.writePage(tmpPage);
        result.add(tempID);
//        System.out.println("   result " + result);
//        System.out.println("page " + page + ": (" + page.getNodes().size() + ") " + page.getNodes());
      }
    }

    if (DEBUG) {
      msg.append("\n   s' = " + result);
      logger.info(msg.toString());
      System.out.println("   s' = " + result);
    }
    return result;
  }

  private SlotPage<K, V> getPage(int pageID) {
    SlotPage<K, V> page = file.readPage(pageID);
    if (page == null) {
      page = new SlotPage<K, V>(B);
      page.setID(pageID);
      file.writePage(page);
    }
    return page;
  }

  private void store(List<Integer> s, SlotPage<K, V> page) {
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\n   store objects of page " + s + " in page " + page);
    }

    int tmpPageID = s.remove(0);
    SlotPage<K, V> tmpPage = file.readPage(tmpPageID);
    page.insertAll(tmpPage.getNodes());
    file.deletePage(tmpPageID);
    file.writePage(page);

    if (DEBUG) {
      logger.info(msg.toString());
//      System.out.println(msg);
    }

    if (! s.isEmpty()) {
      page = getPage(page.getID() + 1);
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
      // number of pages of the slots in level i
      int l_i = (int) (Math.pow(mu + 1, i - 1) * (this.maxSize_h1 / 2 / B));
//      System.out.println("level " + i + " # pages " + l_i);

      // number of pages in all slots in level i
      if (i < level) {
        pageNumber += mu * l_i;
      }
      // number of pages from slot_1 to the specified slot in level i
      else {
        pageNumber += (slot - 1) * l_i;
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
    HeapNode<K, V> h1_min = h1.getNodeAt(0);
    System.out.println("");
    System.out.println("h1[0] " + h1_min);

    int minIndex = -1;
    HeapNode<K, V> currentMin = h1_min;

    for (int i = 0; i < h2.length; i++) {
      SlotPage<K, V> slotPage = h2[i];
      if (slotPage != null && ! slotPage.getNodes().isEmpty()) {
        HeapNode<K, V> h2_min = slotPage.getNodes().get(0);
        if (h2_min.compareTo(currentMin) < 0) {
          minIndex = i;
          currentMin = h2_min;
        }
        System.out.println("h2[" + i + "] " + slotPage.getNodes().get(0));
      }
    }

    if (minIndex == -1) return h1.getMinNode();

    SlotPage<K, V> slotPage = h2[minIndex];
    HeapNode<K, V> h2_min = slotPage.getNodes().remove(0);

    if (slotPage.isEmpty()) {
      System.out.println(this);
      int level = minIndex / mu + 1;
      int slot = minIndex % mu + 1;
      System.out.println("minIndex " + minIndex);
      System.out.println("level " + level);
      System.out.println("slot " + slot);

      int numberOfPages = (int) (Math.pow(mu + 1, level - 1) * (this.maxSize_h1 / 2 / B));
      int pageID = getFirstPageID(level, slot);

      int nextPageID = pageID + 1;
      SlotPage<K,V> nextPage = file.readPage(nextPageID);
      System.out.println("nextPageID " + nextPageID);
      System.out.println("nextPage " + nextPage);
      System.out.println("nextPage.isEmpty " + nextPage.isEmpty());
      System.out.println("pageID " + pageID);
      System.out.println("numberOfPages " + numberOfPages);

      while (nextPage.isEmpty() && pageID + numberOfPages > nextPageID + 1) {
        nextPageID++;
        nextPage = file.readPage(nextPageID);
        System.out.println("  nextPageID " + nextPageID);
        System.out.println("  nextPage " + nextPage);
      }
      System.out.println("nextPage " + nextPage);
      if (! nextPage.isEmpty()) {
        System.out.println("nextPage not empty " + nextPage);
        h2[minIndex].insertAll(nextPage.getNodes());
        nextPage.clearNodes();
        file.writePage(nextPage);
      }
    }

    return h2_min;
  }

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  public boolean isEmpty() {
    return size == 0;
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
    return size;
  }


  public static void main(String[] args) {
    ArrayHeap<Integer, Identifiable> heap1 = new ArrayHeap<Integer, Identifiable>(4000, 80000, 8);
    PersistentHeap<Integer, DefaultIdentifiable> heap2 = new PersistentHeap<Integer, DefaultIdentifiable>(4000, 80000, 8);
    DefaultHeap<Integer, Identifiable> heap3 = new DefaultHeap<Integer, Identifiable>();

    Random random = new Random(210571);

    for (int i = 0; i < 10000; i++) {
      int key = random.nextInt(100000);

      if (i % 1000 == 0) {
        System.out.println("i " + i);
      }
      heap1.addNode(new DefaultHeapNode<Integer, Identifiable>(key, new DefaultIdentifiable(i)));
      heap2.addNode(new DefaultHeapNode<Integer, DefaultIdentifiable>(key, new DefaultIdentifiable(i)));
      heap3.addNode(new DefaultHeapNode<Integer, Identifiable>(key, new DefaultIdentifiable(i)));
    }

    for (int i = 0; i < 10000; i++) {
      HeapNode<Integer, Identifiable> n1 = heap1.getMinNode();
      HeapNode<Integer, DefaultIdentifiable> n2 = heap2.getMinNode();
      HeapNode<Integer, Identifiable> n3 = heap3.getMinNode();

      System.out.println("n3[" + i + "] = " + n3);
      if (! n1.getKey().equals(n3.getKey())) {
        System.out.println("i " + i);
        System.out.println("n2[" + i + "] = " + n2);
        System.out.println("key n1 != n3 " + n1 + " != " + n3);
        throw new RuntimeException();
      }
      if (! n1.getValue().equals(n3.getValue())) {
        System.out.println("i " + i);
        System.out.println("n2[" + i + "] = " + n2);
        System.out.println("value n1 != n3 " + n1 + " != " + n3);
        throw new RuntimeException();
      }
      if (! n2.getKey().equals(n3.getKey())) {
        System.out.println("i " + i);
        System.out.println("n1[" + i + "] = " + n1);
        System.out.println("key n2 != n3 " + n2 + " != " + n3);
        throw new RuntimeException();
      }
      if (! n2.getValue().equals(n3.getValue())) {
        System.out.println("i " + i);
        System.out.println("n1[" + i + "] = " + n1);
        System.out.println("value n2 != n3 " + n2 + " != " + n3);
        throw new RuntimeException();
      }
    }

    System.out.println("heap 1 " + heap1.file.getIOAccess());
    System.out.println("heap 2 " + heap2.getIOAccess());
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
    result.append("\nH1[" + h1.size() + "] " + h1);
    result.append("\nH2[" + h2.length + "] " + Arrays.asList(h2));

    for (int level = 1; level <= L; level++) {
      result.append("\nLevel " + level);
      for (int j = 1; j <= mu; j++) {
        int l_i = (int) (Math.pow(mu + 1, level - 1) * (this.maxSize_h1 / 2 / B));
        int pageID = getFirstPageID(level, j);
        result.append("\n   slot " + j + ": ");
        for (int p = 0; p < l_i; p++) {
          SlotPage<K, V> page = file.readPage(p + pageID);
          if (page != null)
            if (! page.isEmpty())
              result.append((p + pageID) + " ");
            else
              result.append((p + pageID) + "* ");
        }
      }
    }

    result.append("\nI/O " + file.getIOAccess());
    return result.toString();
  }


}
