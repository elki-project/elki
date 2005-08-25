package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.index.btree.BTreeData;
import de.lmu.ifi.dbs.utilities.heap.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

/**
 * Wrapper class for testing a persistent priority queue.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PriorityQueue {

  static int NODE_SIZE = 40;
  static int PAGE_SIZE = 100 * NODE_SIZE;
  static int CACHE_SIZE = 50 * PAGE_SIZE;

  public static void main(String[] args) {
    System.out.println("NODE_SIZE  = " + NODE_SIZE);
    System.out.println("PAGE_SIZE  = " + PAGE_SIZE);
    System.out.println("");
    System.out.println("#_Objects #_Pages #_Pages_in_Cache I/O_PQ I/O_BTree CPU_PQ CPU_BTree");

    for (int i = 1; i < 11; i++) {
      int numObjects = 5000 * i;
      Random random = new Random(29);
      Hashtable<Integer, Integer> keys = new Hashtable<Integer, Integer>();

      while (keys.size() < numObjects) {
        int k = random.nextInt();
        if (keys.containsKey(k)) continue;
        keys.put(k, k);
      }
//    System.out.println(Arrays.asList(keys));

      CACHE_SIZE = NODE_SIZE * numObjects / 2;
      StringBuffer msg = new StringBuffer();
      msg.append(numObjects);
      msg.append(" ");
      msg.append(NODE_SIZE * numObjects / PAGE_SIZE);
      msg.append("    ");
      msg.append(CACHE_SIZE / PAGE_SIZE);

      long[] pq = testPQ(keys);
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
    Date start = new Date();
    PersistentHeap<Integer, Integer> pq = new PersistentHeap<Integer, Integer>(PAGE_SIZE,
                                                                               CACHE_SIZE,
                                                                               NODE_SIZE);
    int i = 0;
    for (Integer key : keys.keySet()) {
      HeapNode<Integer, Integer> node =
      new DefaultHeapNode<Integer, Integer>(key, key);
//      if (++i % 1000 == 0) System.out.println("i " + i);
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
//      if (++i % 1000 == 0) System.out.println("i " + i);
    }

    long ms = new Date().getTime() - start.getTime();
    return new long[]{pq.getIOAccess(), ms};
  }

  private static long[] testBTree(Hashtable<Integer, Integer> keys) {
    Date start = new Date();
    int m = PAGE_SIZE / NODE_SIZE / 2;
    BTree<Integer, Integer> btree = new BTree<Integer, Integer>(m,
                                                                PAGE_SIZE,
                                                                CACHE_SIZE);
    int i = 0;
    for (Integer key : keys.keySet()) {
      btree.insert(key, key);
//      if (++i % 1000 == 0) System.out.println("i " + i);
    }
//    System.out.println(btree.printStructure());
//    long ms = new Date().getTime() - start.getTime();
//    System.out.println("\n*************************************************");
//    System.out.println("BTree add  : " + ms + " ms = " + Util.format(((double) ms) / 1000));
//    System.out.println(" I/O  add  : " + btree.getIOAccess());

    BTreeData<Integer, Integer> min = btree.getMinimum();
    i = 1;
    while (min != null) {
      min = btree.getMinimum();
//      if (++i % 1000 == 0) System.out.println("i " + i);
    }
    long ms = new Date().getTime() - start.getTime();
//    System.out.println("BTree total: " + ms + " ms = " + Util.format(((double) ms) / 1000));
//    System.out.println(" I/O  total: " + btree.getIOAccess());

    return new long[]{btree.getIOAccess(), ms};
  }

  private static void testMinMax(Hashtable<Integer, Integer> keys) {
    Date start = new Date();
    MinMaxHeap<Integer, Integer> heap = new MinMaxHeap<Integer, Integer>();

    for (Integer key : keys.keySet()) {
      HeapNode<Integer, Integer> node =
      new DefaultHeapNode<Integer, Integer>(key, key);
//      System.out.println("node " + node);
      heap.addNode(node);
    }
    System.out.println("PQ add " + (new Date().getTime() - start.getTime()) + " ms");
    System.out.println(heap);

    while (! heap.isEmpty()) {
      HeapNode<Integer, Integer> min = heap.getMinNode();
      System.out.println("min " + min);
//      System.out.println(minMaxHeap);
    }
    System.out.println("PQ total " + (new Date().getTime() - start.getTime()) + " ms");
  }

  private static void testHeap(Hashtable<Integer, Integer> keys) {
    Date start = new Date();
    DefaultHeap<Integer, Integer> heap = new DefaultHeap<Integer, Integer>();

    int i = 0;
    for (Integer key : keys.keySet()) {
      HeapNode<Integer, Integer> node =
      new DefaultHeapNode<Integer, Integer>(key, key);
//      System.out.println("node " + node);
      heap.addNode(node);
    }
    System.out.println("PQ add " + (new Date().getTime() - start.getTime()) + " ms");
    System.out.println(heap);

    while (! heap.isEmpty()) {
      HeapNode<Integer, Integer> min = heap.getMinNode();
      System.out.println("min " + min);
//      System.out.println(minMaxHeap);
    }
    System.out.println("PQ total " + (new Date().getTime() - start.getTime()) + " ms");
  }

  private static void testElkiHeap(Hashtable<Integer, Integer> keys) {
    Date start = new Date();
    MinMaxHeap heap = new MinMaxHeap();

    int i = 0;
    for (Integer key : keys.keySet()) {
      HeapNode<Integer, Integer> node =
      new DefaultHeapNode<Integer, Integer>(key, key);
//      System.out.println("node " + node);
      heap.addNode(node);
    }
    System.out.println("PQ add " + (new Date().getTime() - start.getTime()) + " ms");
    System.out.println(heap);

    while (! heap.isEmpty()) {
      HeapNode<Integer, Integer> min = heap.getMinNode();
      System.out.println("min " + min);
//      System.out.println(minMaxHeap);
    }
    System.out.println("PQ total " + (new Date().getTime() - start.getTime()) + " ms");
  }


}
