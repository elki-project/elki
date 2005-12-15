package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.index.btree.BTreeData;
import de.lmu.ifi.dbs.utilities.heap.*;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Random;
import java.util.Arrays;

/**
 * Wrapper class for testing a persistent priority queue.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PriorityQueue {

  static int NODE_SIZE = 8;
  static int PAGE_SIZE = 32 * 1024;
  static int CACHE_SIZE = 16 * 1024 * 1024;
//  static int PAGE_SIZE = 300 * NODE_SIZE;
//  static int CACHE_SIZE = 50 * PAGE_SIZE;

  /*
  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    int m = PAGE_SIZE / NODE_SIZE / 2;
    BTree<Integer, Integer> btree = new BTree<Integer, Integer>(m,PAGE_SIZE,CACHE_SIZE);

    for (int i = 0; i < 10000; i++) {
      btree.insert(i,i);
      if (i % 1000 == 0)
      System.out.println(i / 1000);
    }

    long end = System.currentTimeMillis();
    long total = (end - start) / 1000;
    System.out.println(total + "  s");

  }
  */

  public static void main(String[] args) {
    System.out.println("NODE_SIZE   = " + NODE_SIZE);
    System.out.println("PAGE_SIZE   = " + PAGE_SIZE);
    System.out.println("CACHE_SIZE  = " + CACHE_SIZE);
    System.out.println("");
    System.out.println("#_Objects #_Pages #_Pages_in_Cache I/O_PQ I/O_BTree CPU_PQ CPU_BTree");

    for (int i = 1; i < 2; i++) {
      int numObjects = 1000000 * i;
      Random random = new Random(29);
      Hashtable<Integer, Integer> keys = new Hashtable<Integer, Integer>();

      while (keys.size() < numObjects) {
        int k = random.nextInt();
        if (keys.containsKey(k)) continue;
        keys.put(k, k);
      }
//    System.out.println(Arrays.asList(keys));
                  
      StringBuffer msg = new StringBuffer();
      msg.append(numObjects);
      msg.append(" ");
      msg.append(NODE_SIZE * numObjects / PAGE_SIZE + 1);
      msg.append("    ");
      msg.append(CACHE_SIZE / PAGE_SIZE);

      System.out.println(msg.toString());
      long[] pq = testPQ(keys);

      System.out.println(msg.toString() + "\n" + pq[0] + " "  + pq[1]);

      long[] btree = testBTree(keys);

      msg.append("    ");
      msg.append(pq[0]);
      msg.append("    ");
      msg.append(btree[0]);
      msg.append("    ");
      msg.append(pq[1]);
      msg.append("    ");
      msg.append(btree[1]);
      msg.append("\n");

      System.out.println(msg);
    }

//    testMinMax(keys);
//    testElkiHeap(keys);
//    testHeap(keys);
  }

  private static long[] testPQ(Hashtable<Integer, Integer> keys) {
    long start = System.currentTimeMillis();
    PersistentHeap<Integer, DefaultIdentifiable> pq = new PersistentHeap<Integer, DefaultIdentifiable>(PAGE_SIZE,
                                                                     CACHE_SIZE,
                                                                     NODE_SIZE);
    int i = 0;
    for (Integer key : keys.keySet()) {
      HeapNode<Integer, DefaultIdentifiable> node =
      new DefaultHeapNode<Integer, DefaultIdentifiable>(key, new DefaultIdentifiable(key));
      if (++i % 1000 == 0) System.out.println("add i " + i);
//      System.out.println("node " + node);
      pq.addNode(node);
    }
//    long ms = new Date().getTime() - start.getTime();
//    System.out.println("\n*************************************************");
//    System.out.println("PQ  add  : " + ms + " ms = " + Util.format(((double) ms) / 1000) + " s");
//    System.out.println("I/O add  : " + pq.getIOAccess());
//    System.out.println(pq);

    i = 0;
    while (! pq.isEmpty()) {
      pq.getMinNode();
      if (++i % 1000 == 0) System.out.println("del i " + i);
    }

    long ms = System.currentTimeMillis() - start;
    return new long[]{pq.getIOAccess(), ms};
  }

  private static long[] testBTree(Hashtable<Integer, Integer> keys) {
    long start = System.currentTimeMillis();
    int m = PAGE_SIZE / NODE_SIZE / 2;
    BTree<Integer, DefaultIdentifiable> btree = new BTree<Integer, DefaultIdentifiable>(m,
                                                      PAGE_SIZE,
                                                      CACHE_SIZE);
    int i = 0;
    for (Integer key : keys.keySet()) {
      btree.insert(key, new DefaultIdentifiable(key));
//      if (++i % 1000 == 0) System.out.println("i " + i);
    }
//    System.out.println(btree.printStructure());
//    long ms = new Date().getTime() - start.getTime();
//    System.out.println("\n*************************************************");
//    System.out.println("BTree add  : " + ms + " ms = " + Util.format(((double) ms) / 1000));
//    System.out.println(" I/O  add  : " + btree.getIOAccess());

    BTreeData<Integer, DefaultIdentifiable> min = btree.getMinimum();
    i = 1;
    while (min != null) {
      min = btree.getMinimum();
//      if (++i % 1000 == 0) System.out.println("i " + i);
    }
    long ms = System.currentTimeMillis() - start;
//    System.out.println("BTree total: " + ms + " ms = " + Util.format(((double) ms) / 1000));
//    System.out.println(" I/O  total: " + btree.getIOAccess());

    return new long[]{btree.getIOAccess(), ms};
  }

  private static void testMinMax(Hashtable<Integer, Integer> keys) {
    long start = System.currentTimeMillis();
    MinMaxHeap<Integer, DefaultIdentifiable> heap = new MinMaxHeap<Integer, DefaultIdentifiable>();

    for (Integer key : keys.keySet()) {
      HeapNode<Integer, DefaultIdentifiable> node =
      new DefaultHeapNode<Integer, DefaultIdentifiable>(key, new DefaultIdentifiable(key));
//      System.out.println("node " + node);
      heap.addNode(node);
    }
    System.out.println("PQ add " + (System.currentTimeMillis() - start) + " ms");
    System.out.println(heap);

    while (! heap.isEmpty()) {
      HeapNode<Integer, DefaultIdentifiable> min = heap.getMinNode();
      System.out.println("min " + min);
//      System.out.println(minMaxHeap);
    }
    System.out.println("PQ total " + (System.currentTimeMillis() - start) + " ms");
  }

  private static void testHeap(Hashtable<Integer, Integer> keys) {
    long start = System.currentTimeMillis();
    DefaultHeap<Integer, DefaultIdentifiable> heap = new DefaultHeap<Integer, DefaultIdentifiable>();

    int i = 0;
    for (Integer key : keys.keySet()) {
      HeapNode<Integer, DefaultIdentifiable> node =
      new DefaultHeapNode<Integer, DefaultIdentifiable>(key, new DefaultIdentifiable(key));
//      System.out.println("node " + node);
      heap.addNode(node);
    }
    System.out.println("PQ add " + (System.currentTimeMillis() - start) + " ms");
    System.out.println(heap);

    while (! heap.isEmpty()) {
      HeapNode<Integer, DefaultIdentifiable> min = heap.getMinNode();
      System.out.println("min " + min);
//      System.out.println(minMaxHeap);
    }
    System.out.println("PQ total " + (System.currentTimeMillis() - start) + " ms");
  }

  private static void testElkiHeap(Hashtable<Integer, Integer> keys) {
    long start = System.currentTimeMillis();
    MinMaxHeap<Integer, Identifiable>heap = new MinMaxHeap<Integer, Identifiable>();

    int i = 0;
    for (Integer key : keys.keySet()) {
      HeapNode<Integer, Identifiable> node =
      new DefaultHeapNode<Integer, Identifiable>(key, new DefaultIdentifiable(key));
//      System.out.println("node " + node);
      heap.addNode(node);
    }
    System.out.println("PQ add " + (System.currentTimeMillis() - start) + " ms");
    System.out.println(heap);

    while (! heap.isEmpty()) {
      HeapNode<Integer, Identifiable> min = heap.getMinNode();
      System.out.println("min " + min);
//      System.out.println(minMaxHeap);
    }
    System.out.println("PQ total " + (System.currentTimeMillis() - start) + " ms");
  }
}
