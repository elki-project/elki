package de.lmu.ifi.dbs.utilities.heap;

import de.lmu.ifi.dbs.utilities.Identifiable;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Implementation of a heap-based priority queue. This heap orders elements
 * according to their natural order. Elements stored in this heap must be
 * instances of <code>HeapNode<\code>
 * (@see HeapNode).
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultHeap<K extends Comparable<K>, V extends Identifiable> implements Heap<K, V> {
  /**
   * Indicates the index null.
   */
  private final static int NULL_INDEX = -1;

  /**
   * Contains all elements of the heap.
   */
  private Vector<HeapNode<K, V>> heap;

  /**
   * Holds the indices in the heap of each element.
   */
  private Hashtable<Integer, Integer> indices;

  /**
   * Indicates weather this heap is organised in ascending or descending
   * order.
   */
  private boolean ascending = true;

  /**
   * Creates a new heap that stores the elements in ascending order.
   */
  public DefaultHeap() {
    this(true);
  }

  /**
   * Creates a new heap that stores the elements in the specified order.
   *
   * @param ascending if true, the heap is organised in ascending order, otherwise
   *                  the heap is organised in descending other
   */
  public DefaultHeap(boolean ascending) {
    this.heap = new Vector<HeapNode<K, V>>();
    this.indices = new Hashtable<Integer, Integer>();
    this.ascending = ascending;
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public void addNode(final HeapNode<K, V> node) {
    if (indices.containsKey(node.getValue().getID()))
      throw new IllegalArgumentException("Node " + node
                                         + " already exists in this heap!");

    int lastIndex = heap.size();
    heap.add(node);
    indices.put(node.getValue().getID(), lastIndex);

    flowUp(lastIndex);
  }

  /**
   * Retrieves and removes the minimum node of this heap. If the heap is
   * empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public HeapNode<K, V> getMinNode() {
    if (isEmpty())
      return null;
    return removeMin();
  }

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  public final boolean isEmpty() {
    return heap.size() == 0;
  }

  /**
   * Returns the current index of the specified value in this heap.
   *
   * @param value the value for which the index should be returned
   * @return the current index of the specified value in this heap
   */
  public Integer getIndexOf(V value) {
    return indices.get(value.getID());
  }

  /**
   * Returns the node at the specified index.
   *
   * @param index the index of the node to be returned
   * @return the node at the specified index
   */
  public final HeapNode<K, V> getNodeAt(final int index) {
    return heap.get(index);
  }

  /**
   * Moves up a node at the specified index until it satisfies the heaporder.
   *
   * @param index the index of the node to be moved up.
   */
  public void flowUp(int index) {
    // swap the key at index with its parents along the path to the root
    // until it finds the place, where the heaporder is fulfilled.
    while (parent(index) != NULL_INDEX && isLowerThan(index, parent(index))) {
      swap(parent(index), index);
      index = parent(index);
    }
  }

  /**
   * Returns the size of this heap.
   *
   * @return the size of this heap
   */
  public int size() {
    return heap.size();
  }

  /**
   * Clears the heap.
   */
  public void clear() {
    heap.clear();
    indices.clear();
  }

  /**
   * Returns a copy of the vector holding this heap.
   * @return a copy of the vector holding this heap
   */
  public Vector<HeapNode<K,V>> copy() {
    return (Vector<HeapNode<K,V>>) heap.clone();
  }

  /**
   * Moves down a node at index i until it satisfies the heaporder.
   *
   * @param i The index of the node to be moved down.
   */
  protected final void flowDown(int i) {
    // swap the key at i with its parents along the path to the root
    // until it finds the place, where the heaporder is fulfilled.

    while (minChild(i) != NULL_INDEX && isGreaterThan(i, minChild(i))) {
      int minChild = minChild(i);
      swap(minChild(i), i);
      i = minChild;
    }
  }

  /**
   * Removes and returns the minimum node from this heap and restores the
   * heap. Returns The minimum node of this heap.
   *
   * @return the minimum node from this heap
   */
  protected HeapNode<K, V> removeMin() {
    // move minimum node to the end
    int lastIndex = heap.size() - 1;
    swap(0, lastIndex);
    HeapNode<K, V> result = heap.get(lastIndex);
    heap.remove(lastIndex);

    // actualize indices
    indices.remove(result.getValue().getID());

    // restore the heap from the root on
    heapify(0);

    return result;
  }

  /**
   * Swaps the nodes at the indices i1 and i2 in the array.
   *
   * @param i1
   * @param i2
   */
  protected final void swap(final int i1, final int i2) {
    // get both elements
    HeapNode<K, V> first = heap.get(i1);
    HeapNode<K, V> second = heap.get(i2);

    // swap them
    heap.setElementAt(first, i2);
    heap.setElementAt(second, i1);

    // actualize indices
    indices.put(first.getValue().getID(), i2);
    indices.put(second.getValue().getID(), i1);
  }

  /**
   * Heapifies the subtree located at i. Precondition: i's both childtrees are
   * heapordered
   *
   * @param i
   */
  protected final void heapify(int i) {
    // move the key down the tree till we're done.
    while (i != NULL_INDEX) {
      i = heapifyLocally(i);
    }
  }

  /**
   * Heap order node at index i with respect to its both children. If keys had
   * to be swapped return the new index of the node formerly located at i,
   * nullIndex otherwise
   *
   * @param i The index of the node to be heapified.
   * @return The new index of the node formerly located at i if keys had to be
   *         swapped, nullIndex otherwise
   */
  protected final int heapifyLocally(final int i) {
    final int min = minChild(i);
    if (min == NULL_INDEX) {
      return NULL_INDEX;
    } // i is leaf. we're done.

    // if max child has bigger key then swap
    if (isLowerThan(min, i)) {
      swap(i, min);
      return min;
    }
    else {
      return NULL_INDEX;
    }
  }

  /**
   * Returns the index of the smaller child of the node at index i. Returns
   * nullIndex if the node is a leaf.
   *
   * @param i The index of the node
   * @return nullIndex if the node is a leaf, the index of the smaller child
   *         otherwise.
   */
  protected final int minChild(final int i) {
    final int right = rightChild(i);
    final int left = leftChild(i);

    if (right == NULL_INDEX) {
      return left;
    }
    else { // because heap is complete there must be a left child
      return isLowerThan(left, right) ? left : right;
    }
  }

  /**
   * Return the index of the left child of of the node at index i, nullIndex
   * if there is no such child.
   *
   * @param i The index of the node.
   * @return The index of the left child of of the node at index i, nullIndex
   *         if there is no such child.
   */
  protected final int leftChild(final int i) {
    return indexOf(2 * i + 1); // Because root is at 0. Root at 1 gives
    // 2*i.
  }

  /**
   * Return the index of the right child of of the node at index i, nullIndex
   * if there is no such child.
   *
   * @param i The index of the node.
   * @return The index of the right child of of the node at index i, nullIndex
   *         if there is no such child.
   */
  protected final int rightChild(final int i) {
    return indexOf(2 * i + 2); // Because root is at 0. Root at 1 gives 2*i
    // +1.
  }

  /**
   * Returns the index of the parent of the node at index k, nullIndex if k is
   * the root.
   *
   * @param k The index of the node.
   * @return The parent of the node at index k, nullIndex if k is the root
   */
  protected final int parent(final int k) {
    return k == 0 ? NULL_INDEX : indexOf((k - 1) / 2); // Because root is
    // at 0. Root at 1
    // gives k/2.
    // parent(root) is now root. This makes ?: necessary
  }

  /**
   * Return the index of the node at index i in this heap if i is in heap,
   * otherwise nullIndex.
   *
   * @param i The index of the node.
   * @return i if the index is in heap, otherwise nullIndex
   */
  protected final int indexOf(final int i) {
    return i >= 0 && i <= heap.size() - 1 ? i : NULL_INDEX;
  }

  /**
   * Return true if the node at index i1 is lower than the node at index i2.
   *
   * @param i1 The index of the first node to be tested.
   * @param i2 The index of the second node to be tested.
   * @return True if the node at index i1 is lower than the node at index i2,
   *         false otherwise.
   */
  protected final boolean isLowerThan(final int i1, final int i2) {
    if (ascending)
      return heap.get(i1).compareTo(heap.get(i2)) < 0;
    else
      return heap.get(i1).compareTo(heap.get(i2)) > 0;
  }

  /**
   * Return true if the node at index i1 is lower than the node at index i2.
   *
   * @param i1 The index of the first node to be tested.
   * @param i2 The index of the second node to be tested.
   * @return True if the node at index i1 is lower than the node at index i2,
   *         false otherwise.
   */
  protected final boolean isGreaterThan(final int i1, final int i2) {
    if (ascending)
      return heap.get(i1).compareTo(heap.get(i2)) > 0;
    else
      return heap.get(i1).compareTo(heap.get(i2)) < 0;
  }

  /**
   * Returns a string representation of this heap.
   *
   * @return a string representation of this heap
   */
  public String toString() {
    return heap.toString();
  }

  /**
   * For debugging purposes
   */
  public void test() {
    for (int i = 0; i < heap.size(); i++) {
      HeapNode<K, V> node = heap.get(i);
      int index = indices.get(node.getValue().getID());
      if (index != i) {
        System.out.println("Node " + node);
        System.out.println("index " + i + " != indices " + index);
        System.exit(1);
      }
    }
  }

  public static void main(String[] args) {
    /*
    * DefaultHeap<Integer, Integer> heap = new DefaultHeap<Integer,
    * Integer>();
    *
    * heap.addNode(new DefaultHeapNode<Integer, Integer>(2, 2));
    * heap.addNode(new DefaultHeapNode<Integer, Integer>(4, 4));
    * heap.addNode(new DefaultHeapNode<Integer, Integer>(4, 4));
    * heap.addNode(new DefaultHeapNode<Integer, Integer>(6, 6));
    * heap.addNode(new DefaultHeapNode<Integer, Integer>(1, 1));
    * heap.addNode(new DefaultHeapNode<Integer, Integer>(3, 3));
    *
    * while (! heap.isEmpty()) { System.out.println(heap.getMinNode()); }
    */
  }

}
