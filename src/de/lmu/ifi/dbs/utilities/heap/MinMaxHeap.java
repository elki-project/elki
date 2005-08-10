package de.lmu.ifi.dbs.utilities.heap;

import java.util.Arrays;

/**
 * A double-ended priority queue implemented as a binary heap. This heap
 * provides access to the minimum and the maximums elements in the queue.
 * Elements stored in this heap must be instances of  <code>HeapNode<\code>
 * (@see HeapNode).
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MinMaxHeap implements Heap {

  /**
   * Indicates the index null.
   */
  final static int nullIndex = -1;

  /**
   * Array holding the heap.
   */
  HeapNode[] array;

  /**
   * Points to the last node in this heap.
   */
  protected int lastHeap = nullIndex;

  /**
   * The length of this heap.
   */
  protected int length;

  /**
   * Indicates if this heap is resizeable or has a fixed length.
   */
  private boolean resizeable;


  /**
   * Creates a new heap and initializes it as empty heap of the specified length.
   * The heap will be resizeable.
   *
   * @param length the length of the heap
   */
  public MinMaxHeap(final int length) {
    this.array = new HeapNode[length];
    this.length = length;
  }

  /**
   * Creates a new heap and initializes it as empty heap of the specified length.
   * According to the specified flag the heap will be resizeable or not.
   *
   * @param length     the length of the heap
   * @param resizeable if true, the heap will be resizeable, otherwise the
   *                   length of the heap is fixed
   */
  public MinMaxHeap(final int length, boolean resizeable) {
    this.array = new HeapNode[length];
    this.length = length;
    this.resizeable = resizeable;
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public synchronized void addNode(final HeapNode node) {
    if (lastHeap == array.length - 1) {
      increaseArray();
    }
    array[++lastHeap] = node;
    flowUp(lastHeap);
  }

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public synchronized HeapNode getMinNode() {
    if (isEmpty()) return null;
    final HeapNode minNode = removeMin();
    minNode.setIndex(-1);
    return minNode;
  }

  /**
   * Retrieves and removes the maximum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return The maximum node of this heap, null in case of emptyness.
   */
  public synchronized HeapNode getMaxNode() {
    if (isEmpty()) return null;
    final HeapNode maxNode = removeMax();
    maxNode.setIndex(-1);
    return maxNode;
  }

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  public final boolean isEmpty() {
    return lastHeap == nullIndex;
  }

  /**
   * Clears this heap.
   */
  public final void clear() {
    array = new HeapNode[array.length];
    lastHeap = nullIndex;
  }

  /**
   * Returns the node at the specified index.
   *
   * @param index the index of the node to be returned.
   * @return the node at the specified index
   */
  public final HeapNode getNodeAt(final int index) {
    return array[index];
  }

  /**
   * Retrieves, but does not remove, the minmum node of
   * this heap. If the heap is empty, null will be returned.
   *
   * @return The minimum node of this heap, null in case of emptyness.
   */
  public synchronized final HeapNode minNode() {
    if (isEmpty()) return null;
    return getNodeAt(0);
  }

  /**
   * Retrieves, but does not remove, the maximum node of
   * this heap. If the heap is empty, null will be returned.
   *
   * @return The maximum node of this heap, null in case of emptyness.
   */
  public synchronized final HeapNode maxNode() {
    if (isEmpty()) return null;
    return getNodeAt(maxIndex());
  }

  /**
   * Moves up or down the node at the specified index until it satisfies the heaporder.
   *
   * @param index the index of the node to be moved up.
   */
  private void flowUp(int index) {
    int parent = parent(index);
    if (parent == nullIndex) return;

    // min level
    if (isMinLevel(index)) {
      if (isGreaterThan(index, parent)) {
        swap(index, parent);
        bubbleUpMax(parent);
      }
      else
        bubbleUpMin(index);
    }
    // max level
    else {
      if (isLowerThan(index, parent)) {
        swap(index, parent);
        bubbleUpMin(index);
      }
      else
        bubbleUpMax(index);
    }
  }

  /**
   * Returns the index of the maximum node of this heap.
   *
   * @return the index of the maximum node of this heap
   */
  private int maxIndex() {
    // get index of max node
    int i;
    if (lastHeap < 2)
      i = lastHeap;
    else
      i = isGreaterThan(1, 2) ? 1 : 2;
    return i;
  }

  /**
   * Removes and returns the minimum node of this heap and restores
   * the heap order.
   *
   * @return the minimum node of this heap
   */
  protected HeapNode removeMin() {
    // move minimum node to the end
    swap(0, lastHeap);
    HeapNode minNode = array[lastHeap];
    array[lastHeap] = null;
    // heap is now one node smaller
    lastHeap--;
    // restore the heap from the root on
    if (lastHeap > 0) trickleDownMin(0);
    return minNode;
  }

  /**
   * Removes and returns the maximum node of this heap and restores
   * the heap order.
   *
   * @return the maximum node of this heap
   */
  private HeapNode removeMax() {
    // get index of max node
    int i = maxIndex();

    // move maximum node to the end
    swap(i, lastHeap);
    HeapNode maxNode = array[lastHeap];
    array[lastHeap] = null;
    // heap is now one node smaller
    lastHeap--;
    // restore the heap from the root on
    if (lastHeap > 0) trickleDownMax(i);
    return maxNode;
  }

  /**
   * Returns true if the specified index is a index where minimum nodes are stored,
   * false otherwise.
   *
   * @param index the index to be tested
   * @return true if the specified index is a index where minimum nodes are stored,
   *         false otherwise
   */
  private boolean isMinLevel(int index) {
    ++index;
    while (index >= 4) index /= 4;
    return (index == 1);
  }

  /**
   * Moves the node at the specified index downwards by
   * swapping child and parent as long as a violation of the interval
   * heap property is seen.
   *
   * @param index the index of the node to be moved up
   */
  private void bubbleUpMin(int index) {
    int gp;
    while (index > 2 && isLowerThan(index, gp = grandparent(index))) {
      swap(index, gp);
      index = gp;
    }
  }

  /**
   * Moves the node at the specified index upwards by
   * swapping child and parent as long as a violation of the interval
   * heap property is seen.
   *
   * @param index the index of the node to be moved up
   */
  private void bubbleUpMax(int index) {
    int gp;
    while (index > 6 && isGreaterThan(index, gp = grandparent(index))) {
      swap(index, gp);
      index = gp;
    }
  }

  /**
   * Restore the heap and trickles down the minimum node at the specified index.
   *
   * @param i the index of the node to be trickled down
   */
  private void trickleDownMin(int i) {
    int j;
    int m_size = lastHeap + 1;
    while (m_size >= (j = 2 * i + 2)) {
      int m;
      int k = 2 * j;
      if (m_size >= k) {
        if (m_size > k) {
          int l = k + 2;
          if (m_size >= l) {
            m = (isLowerThan(k - 1, k) ? k - 1 : k);
            m = (isLowerThan(m, l - 1) ? m : l - 1);
            if (m_size > l) m = (isLowerThan(m, l) ? m : l);
          }
          else {
            m = (isLowerThan(k - 1, k) ? k - 1 : k);
            m = (isLowerThan(m, j) ? m : j);
          }
        }
        else {
          m = (isLowerThan(k - 1, j) ? k - 1 : j);
        }
      }
      else {
        if (m_size > j) {
          m = (isLowerThan(j - 1, j) ? j - 1 : j);
        }
        else {
          m = j - 1;
        }
      }
      if (m > j) {
        if (isLowerThan(m, i)) {
          swap(m, i);
          int p = parent(m);
          if (isGreaterThan(m, p)) swap(m, p);
        }
        else {
          return;
        }
      }
      else {
        if (isLowerThan(m, i)) swap(m, i);
        return;
      }
      i = m;
    }
  }

  /**
   * Restore the heap and trickles down the maximum node at the specified index.
   *
   * @param i the index of the node to be trickled down
   */
  private void trickleDownMax(int i) {
    int j;
    int m_size = lastHeap + 1;
    while (m_size >= (j = 2 * i + 2)) {
      int m;
      int k = 2 * j;
      if (m_size >= k) {
        if (m_size > k) {
          int l = k + 2;
          if (m_size >= l) {
            m = (isGreaterThan(k - 1, k) ? k - 1 : k);
            m = (isGreaterThan(m, l - 1) ? m : l - 1);
            if (m_size > l) m = (isGreaterThan(m, l) ? m : l);
          }
          else {
            m = (isGreaterThan(k - 1, k) ? k - 1 : k);
            m = (isGreaterThan(m, j) ? m : j);
          }
        }
        else {
          m = (isGreaterThan(k - 1, j) ? k - 1 : j);
        }
      }
      else {
        if (m_size > j) {
          m = (isGreaterThan(j - 1, j) ? j - 1 : j);
        }
        else {
          m = j - 1;
        }
      }
      if (m > j) {
        if (isGreaterThan(m, i)) {
          swap(m, i);
          int p = parent(m);
          if (isLowerThan(m, p)) swap(m, p);
        }
        else {
          return;
        }
      }
      else {
        if (isGreaterThan(m, i)) swap(m, i);
        return;
      }
      i = m;
    }
  }

  /**
   * Returns the parent of the node at index i, nullIndex if i is the root.
   *
   * @param i the index of the node
   * @return the parent of the node at index i, nullIndex if i is the root
   */
  private int parent(final int i) {
    return i == 0 ? nullIndex : at((i - 1) / 2);
    // Because root is at 0. Root at 1 gives i/2.
    // parent(root) is now root. This makes ?: necessary
  }

  /**
   * Returns the grandparent of the node at index i, nullIndex if i is the root or parent
   * of i is the root.
   *
   * @param i the index of the node
   * @return the parent of the node at index i, nullIndex if i is the root or parent
   *         of i is the root
   */
  private int grandparent(int i) {
    return i == 2 ? nullIndex : at((i - 3) / 4);
  }

  /**
   * Swaps the nodes at the indices i1 and i2 in the array.
   *
   * @param i1
   * @param i2
   */
  protected final void swap(final int i1, final int i2) {
    final HeapNode t = array[i1];
    array[i1] = array[i2];
    array[i1].setIndex(i1);

    array[i2] = t;
    array[i2].setIndex(i2);
  }

  /**
   * Return true if the node at index i1 is lower than the node at index i2.
   *
   * @param i1 The index of the first node to be tested.
   * @param i2 The index of the second node to be tested.
   * @return True if the node at index i1 is lower than the node at index i2, false otherwise.
   */
  private boolean isLowerThan(final int i1, final int i2) {
    return array[i1].compareTo(array[i2]) < 0;
  }

  /**
   * Return true if the node at index i1 is lower than the node at index i2.
   *
   * @param i1 The index of the first node to be tested.
   * @param i2 The index of the second node to be tested.
   * @return True if the node at index i1 is lower than the node at index i2, false otherwise.
   */
  private boolean isGreaterThan(final int i1, final int i2) {
    return array[i1].compareTo(array[i2]) > 0;
  }

  /**
   * Return the index of the node at index i in this heap if i is in heap, otherwise nullIndex.
   *
   * @param i The index of the node.
   * @return i if the index is in heap, otherwise nullIndex
   */
  private int at(final int i) {
    return i >= 0 && i <= lastHeap ? i : nullIndex;
  }

  /**
   * Increases the underlying array.
   */
  private void increaseArray() {
    if (! resizeable)
      throw new RuntimeException("Size " + length + " is fix and array is full!");

    HeapNode[] tmp = new HeapNode[array.length + length];
    System.arraycopy(array, 0, tmp, 0, array.length);
    array = tmp;
  }

  /**
   * Returns a string representation of this heap.
   *
   * @return a string representation of this heap
   */
  public String toString() {
    return "" + Arrays.asList(array);
  }

}
