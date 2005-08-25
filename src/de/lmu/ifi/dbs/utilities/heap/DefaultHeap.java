package de.lmu.ifi.dbs.utilities.heap;

import java.util.Vector;

/**
 * Implementation of a heap-based priority queue.
 * This heap orders elements according to their natural order.
 * Elements stored in this heap must be instances of  <code>HeapNode<\code>
 * (@see HeapNode).
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultHeap<K extends Comparable<K>, V> implements Heap<K, V> {
  /**
   * Indicates the index null.
   */
  private final static int nullIndex = -1;

  /**
   * Contains all elements of the heap.
   */
  private Vector<HeapNode<K, V>> heap;

  /**
   * Indicates weather this heap is organised in ascending or descending order.
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
    this.ascending = ascending;
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public void addNode(final HeapNode<K, V> node) {
    heap.add(node);

    int lastIndex = heap.size() - 1;
    flowUp(lastIndex);
  }

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public HeapNode<K, V> getMinNode() {
    if (isEmpty()) return null;
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
   * Clears this heap.
   */
  public final void clear() {
    this.heap = new Vector<HeapNode<K, V>>();
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
  protected void flowUp(int index) {
    // swap the key at index with its parents along the path to the root
    // until it finds the place, where the heaporder is fulfilled.
    while (parent(index) != nullIndex && isLowerThan(index, parent(index))) {
      swap(parent(index), index);
      index = parent(index);
    }
  }

  /**
   * Moves down a node at index i until it satisfies the heaporder.
   *
   * @param i The index of the node to be moved down.
   */
  protected final void flowDown(int i) {
    // swap the key at i with its parents along the path to the root
    // until it finds the place, where the heaporder is fulfilled.

    while (minChild(i) != nullIndex && isGreaterThan(i, minChild(i))) {
      int minChild = minChild(i);
      swap(minChild(i), i);
      i = minChild;
    }
  }

  /**
   * Removes and returns the minimum node from this heap and restores the heap.
   * Returns The minimum node of this heap.
   *
   * @return the minimum node from this heap
   */
  protected HeapNode<K, V> removeMin() {
    // move minimum node to the end
    int lastIndex = heap.size() - 1;
    swap(0, lastIndex);
    HeapNode<K, V> result = heap.get(lastIndex);
    heap.remove(lastIndex);

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
    //get both elements
    HeapNode<K, V> first = heap.get(i1);
    HeapNode<K, V> second = heap.get(i2);

    //swap them
    heap.setElementAt(first, i1);
    heap.setElementAt(second, i2);
  }

  /**
   * Heapifies the subtree located at i.
   * Precondition: i's both childtrees are heapordered
   *
   * @param i
   */
  protected final void heapify(int i) {
    // move the key down the tree till we're done.
    while (i != nullIndex) {
      i = heapifyLocally(i);
    }
  }

  /**
   * Heap order node at index i with respect to its both children.
   * If keys had to be swapped return the new index of the node formerly located at i, nullIndex otherwise
   *
   * @param i The index of the node to be heapified.
   * @return The new index of the node formerly located at i if keys had to be swapped, nullIndex otherwise
   */
  protected final int heapifyLocally(final int i) {
    final int min = minChild(i);
    if (min == nullIndex) {
      return nullIndex;
    } // i is leaf. we're done.

    // if max child has bigger key then swap
    if (isLowerThan(min, i)) {
      swap(i, min);
      return min;
    }
    else {
      return nullIndex;
    }
  }

  /**
   * Returns the index of the smaller child of the node at index i. Returns nullIndex if the node is a leaf.
   *
   * @param i The index of the node
   * @return nullIndex if the node is a leaf, the index of the smaller child otherwise.
   */
  protected final int minChild(final int i) {
    final int right = rightChild(i);
    final int left = leftChild(i);

    if (right == nullIndex) {
      return left;
    }
    else { // because heap is complete there must be a left child
      return isLowerThan(left, right) ? left : right;
    }
  }

  /**
   * Return the index of the left child of of the node at index i, nullIndex if there is no such child.
   *
   * @param i The index of the node.
   * @return The index of the left child of of the node at index i, nullIndex if there is no such child.
   */
  protected final int leftChild(final int i) {
    return at(2 * i + 1); // Because root is at 0. Root at 1 gives 2*i.
  }

  /**
   * Return the index of the right child of of the node at index i, nullIndex if there is no such child.
   *
   * @param i The index of the node.
   * @return The index of the right child of of the node at index i, nullIndex if there is no such child.
   */
  protected final int rightChild(final int i) {
    return at(2 * i + 2); // Because root is at 0. Root at 1 gives 2*i +1.
  }

  /**
   * Returns the parent of the node at index k, nullIndex if k is the root.
   *
   * @param k The index of the node.
   * @return The parent of the node at index k, nullIndex if k is the root
   */
  protected final int parent(final int k) {
    return k == 0 ? nullIndex : at((k - 1) / 2); // Because root is at 0. Root at 1 gives k/2.
    // parent(root) is now root. This makes ?: necessary
  }

  /**
   * Return the index of the node at index i in this heap if i is in heap, otherwise nullIndex.
   *
   * @param i The index of the node.
   * @return i if the index is in heap, otherwise nullIndex
   */
  protected final int at(final int i) {
    return i >= 0 && i <= heap.size() - 1 ? i : nullIndex;
  }

  /**
   * Return true if the node at index i1 is lower than the node at index i2.
   *
   * @param i1 The index of the first node to be tested.
   * @param i2 The index of the second node to be tested.
   * @return True if the node at index i1 is lower than the node at index i2, false otherwise.
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
   * @return True if the node at index i1 is lower than the node at index i2, false otherwise.
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

}
