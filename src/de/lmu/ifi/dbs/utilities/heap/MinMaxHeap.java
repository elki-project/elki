package de.lmu.ifi.dbs.utilities.heap;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A double-ended priority queue implemented as a binary heap. This heap
 * provides access to the minimum and the maximums elements in the queue.
 * Elements stored in this heap must be instances of  <code>HeapNode<\code>
 * (@see HeapNode).
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MinMaxHeap<K extends Comparable<K>, V> implements Heap<K, V> {
  /**
   * Indicates the index null.
   */
  private final static int NULL_INDEX = -1;

  /**
   * Contains all elements of the heap.
   */
  Vector<HeapNode<K, V>> heap;

  /**
   * Holds the indices in the heap of each element.
   */
  Hashtable<V, Integer> indices;

  /**
   * Constructs and initialises a new min-max-heap.
   */
  public MinMaxHeap() {
    this.heap = new Vector<HeapNode<K, V>>();
    this.indices = new Hashtable<V, Integer>();
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public void addNode(HeapNode<K, V> node) {
    heap.add(node);
    indices.put(node.getValue(), heap.size() - 1);

    restoreHeap();
  }

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public synchronized HeapNode<K, V> getMinNode() {
    if (isEmpty()) {
      return null;
    }

    // move minimum node to the end
    int lastIndex = heap.size() - 1;
    swap(0, lastIndex);
    HeapNode<K, V> min = heap.get(lastIndex);
    heap.remove(lastIndex);

    // actualize indices
    indices.remove(min.getValue());

    // restore the heap
    trickleDown(0);
    return min;
  }

  /**
   * Retrieves, but does not remove, the minmum node of
   * this heap. If the heap is empty, null will be returned.
   *
   * @return The minimum node of this heap, null in case of emptyness.
   */
  public synchronized final HeapNode<K, V> minNode() {
    if (isEmpty()) return null;
    return getNodeAt(0);
  }

  /**
   * Retrieves and removes the maximum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return The maximum node of this heap, null in case of emptyness.
   */
  public synchronized HeapNode<K, V> getMaxNode() {
    if (heap.size() < 1) {
      return null;
    }

    // move maximum node to the end
    int lastIndex = heap.size() - 1;
    int maxIndex = hasChildren(0) ? getGreaterChild(0) : 0;
    swap(maxIndex, lastIndex);
    HeapNode<K, V> max = heap.get(lastIndex);
    heap.remove(lastIndex);

    // actualize indices
    indices.remove(max.getValue());

    // restore the heap
    trickleDown(maxIndex);
    return max;
  }

  /**
   * Retrieves, but does not remove, the maximum node of
   * this heap. If the heap is empty, null will be returned.
   *
   * @return The maximum node of this heap, null in case of emptyness.
   */
  public synchronized final HeapNode<K, V> maxNode() {
    if (isEmpty()) return null;
    int maxIndex = hasChildren(0) ? getGreaterChild(0) : 0;
    return getNodeAt(maxIndex);
  }

  /**
   * Removes all elements from the heap.
   */
  public void clear() {
    heap.clear();
  }

  /**
   * Returns an element of the heap by the index. If the index is not valid
   * <p/>
   * <code>null</code> is returned.
   *
   * @param index the index of the element
   * @return the element
   */
  public HeapNode<K, V> getNodeAt(int index) {
    if (index >= 0 && index < heap.size()) {
      return heap.get(index);
    }

    return null;
  }

  /**
   * Returns the current index of the specified value in this heap.
   *
   * @param value the value for which the index should be returned
   * @return the current index of the specified value in this heap
   */
  public Integer getIndexOf(V value) {
    return indices.get(value);
  }

  /**
   * Moves up a node at the specified index until it satisfies the heaporder.
   *
   * @param index the index of the node to be moved up.
   */
  public void flowUp(int index) {
    //get the element and its parent
    int parentIndex = getParent(index);
    if (parentIndex == NULL_INDEX) return;

    HeapNode<K, V> element = getNodeAt(index);
    HeapNode<K, V> parent = getNodeAt(parentIndex);

    // max level
    if (isMaxLevel(index)) {
      if (element.compareTo(parent) < 0) {
        swap(index, parentIndex);
        bubbleUpMin(index);
      }
      else
        bubbleUpMax(index);
    }

    // min level
    else {
      if (element.compareTo(parent) > 0) {
        swap(index, parentIndex);
        bubbleUpMax(parentIndex);
      }
      else
        bubbleUpMin(index);
    }
  }

  /**
   * Returns the size of the heap.
   *
   * @return the size
   */
  public int size() {
    return heap.size();
  }

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  public boolean isEmpty() {
    return heap.size() == 0;
  }

  /**
   * Swaps elements so that the min-max-heap condition is fulfilled.
   */
  private void restoreHeap() {
    //check if the min-max-heap condition is already fulfilled
    if (heap.size() > 1) {
      //get the root and the last (new) element
      HeapNode<K, V> root = getNodeAt(0);
      int lastIndex = heap.size() - 1;
      HeapNode<K, V> last = getNodeAt(lastIndex);

      //if the last is less than root swap them
      if ((last.compareTo(root) < 0)) {
        swap(0, lastIndex);
      }

      //bubble up the last element to its right position
      bubbleUp(lastIndex);
    }
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return heap.toString();
  }

  /**
   * Swaps the position of two elements.
   *
   * @param firstIndex  index of the first element
   * @param secondIndex index of the second element
   */
  private void swap(int firstIndex, int secondIndex) {
    //get both elements
    HeapNode<K, V> first = heap.get(firstIndex);
    HeapNode<K, V> second = heap.get(secondIndex);

    //swap them
    heap.setElementAt(first, secondIndex);
    heap.setElementAt(second, firstIndex);

    // actualize indices
    indices.put(first.getValue(), secondIndex);
    indices.put(second.getValue(), firstIndex);
  }

  /**
   * The bubble up method.
   *
   * @param index index of the element to bubble up
   */
  private void bubbleUp(int index) {
    //get the element and its parent
    HeapNode<K, V> element = getNodeAt(index);
    int parentIndex = getParent(index);
    HeapNode<K, V> parent = getNodeAt(parentIndex);

    //check if current level is min or max
    if (isMaxLevel(index)) {
      //check if the element has greater parents
      if (parent != null && parent.compareTo(element) > 0) {
        swap(index, parentIndex);
        bubbleUpMin(parentIndex);
      }
      else {
        bubbleUpMax(index);
      }
    }

    else {
      //check if the element has smaller parents
      if (parent != null && parent.compareTo(element) < 0) {
        swap(index, parentIndex);
        bubbleUpMax(parentIndex);
      }
      else {
        bubbleUpMin(index);
      }
    }
  }

  /**
   * Bubbles up a minimum.
   *
   * @param index index of the element to bubble up
   */
  private void bubbleUpMin(int index) {
    //get the element and its grand parents
    HeapNode<K, V> element = getNodeAt(index);
    int grandParentIndex = getGrandParents(index);
    HeapNode<K, V> grandParent = getNodeAt(grandParentIndex);

    //check if element is smaller than its grand parents
    if (grandParent != null && element.compareTo(grandParent) < 0) {
      swap(index, grandParentIndex);
      bubbleUpMin(grandParentIndex);
    }
  }

  /**
   * Bubbles up a maximum.
   *
   * @param index index of the element to bubble up
   */
  private void bubbleUpMax(int index) {
    //get the element and its grand parent
    HeapNode<K, V> element = getNodeAt(index);
    int grandParentIndex = getGrandParents(index);
    HeapNode<K, V> grandParent = getNodeAt(grandParentIndex);

    //check if element is greater than its grand parent
    if (grandParent != null && element.compareTo(grandParent) > 0) {
      swap(index, grandParentIndex);
      bubbleUpMax(grandParentIndex);
    }
  }

  /**
   * The trickle down method.
   *
   * @param index index of the element to trickle down
   */
  private void trickleDown(int index) {

    if (isMaxLevel(index)) {
      trickleDownMax(index);
    }
    else {
      trickleDownMin(index);
    }
  }

  /**
   * Trickles down a minimum.
   *
   * @param index index of the element to trickle down
   */
  private void trickleDownMin(int index) {
    if (!hasChildren(index)) {
      return;
    }

    int childIndex = getSmallerChild(index);
    int grandChildIndex = getSmallestGrandChild(index);
    HeapNode<K, V> child = getNodeAt(childIndex);
    HeapNode<K, V> grandChild = getNodeAt(grandChildIndex);

    if (hasGrandChildren(index) &&
        grandChild.compareTo(child) < 0) {
      if (grandChild.compareTo(getNodeAt(index)) < 0) {
        swap(grandChildIndex, index);
        if (grandChild.compareTo(getNodeAt(getParent(grandChildIndex))) > 0) {
          swap(grandChildIndex, getParent(grandChildIndex));
        }
        trickleDownMin(grandChildIndex);
      }
    }

    else {
      if (child.compareTo(getNodeAt(index)) < 0) {
        swap(childIndex, index);
      }
    }
  }

  /**
   * Trickles down a maximum.
   *
   * @param index index of the element to trickle down
   */
  private void trickleDownMax(int index) {
    if (!hasChildren(index)) {
      return;
    }

    int childIndex = getGreaterChild(index);
    int grandChildIndex = getGreatestGrandChild(index);
    HeapNode<K, V> child = getNodeAt(childIndex);
    HeapNode<K, V> grandChild = getNodeAt(grandChildIndex);


    if (hasGrandChildren(index) &&
        grandChild.compareTo(child) > 0) {

      if (grandChild.compareTo(getNodeAt(index)) > 0) {
        swap(grandChildIndex, index);
        if (grandChild.compareTo(getNodeAt(getParent(grandChildIndex))) < 0) {
          swap(grandChildIndex, getParent(grandChildIndex));
        }
        trickleDownMax(grandChildIndex);
      }
    }

    else {
      if (child.compareTo(getNodeAt(index)) > 0) {
        swap(childIndex, index);
      }
    }
  }

  /**
   * Returns the index of the smaller child.
   *
   * @param index index of the element
   * @return index of the smaller child
   */
  private int getSmallerChild(int index) {

    int leftChildIndex = getLeftChild(index);
    int rightChildIndex = getRightChild(index);

    HeapNode<K, V> leftChild = getNodeAt(leftChildIndex);
    HeapNode<K, V> rightChild = getNodeAt(rightChildIndex);

    if (leftChild == null || rightChild == null ||
        leftChild.compareTo(rightChild) < 0) {
      return leftChildIndex;
    }

    return rightChildIndex;
  }

  /**
   * Returns the index of the smallest grand child.
   *
   * @param index index of the element
   * @return index of the smallest grand child
   */
  private int getSmallestGrandChild(int index) {

    int smallerLeftIndex = getSmallerChild(getLeftChild(index));
    int smallerRightIndex = hasChildren(getRightChild(index)) ?
                            getSmallerChild(getRightChild(index)) : smallerLeftIndex;

    HeapNode<K, V> smallerLeft = getNodeAt(smallerLeftIndex);
    HeapNode<K, V> smallerRight = getNodeAt(smallerRightIndex);

    if (smallerLeft == null || smallerLeft.compareTo(smallerRight) < 0) {
      return smallerLeftIndex;
    }

    return smallerRightIndex;
  }

  /**
   * Returns the index of the greater child.
   *
   * @param index index of the element
   * @return index of the greater child
   */
  private int getGreaterChild(int index) {

    int leftChildIndex = getLeftChild(index);
    int rightChildIndex = getRightChild(index);

    HeapNode<K, V> leftChild = getNodeAt(leftChildIndex);
    HeapNode<K, V> rightChild = getNodeAt(rightChildIndex);

    if (leftChild == null || rightChild == null ||
        leftChild.compareTo(rightChild) > 0) {
      return leftChildIndex;
    }

    return rightChildIndex;
  }

  /**
   * Returns the index of the greatest grand child.
   *
   * @param index index of the element
   * @return index of the greatest grand child
   */
  private int getGreatestGrandChild(int index) {
    int greaterLeftIndex = getGreaterChild(getLeftChild(index));
    int greaterRightIndex = hasChildren(getRightChild(index)) ?
                            getGreaterChild(getRightChild(index)) : greaterLeftIndex;

    HeapNode<K, V> greaterLeft = getNodeAt(greaterLeftIndex);
    HeapNode<K, V> greaterRight = getNodeAt(greaterRightIndex);

    if (greaterLeft == null || greaterLeft.compareTo(greaterRight) > 0) {
      return greaterLeftIndex;
    }

    return greaterRightIndex;
  }

  /**
   * Returns the index of the parent of the element at the specified index.
   *
   * @param index the index of the element
   * @return the index of the parent element
   */
  private int getParent(int index) {
    if (hasParents(index)) {
      return (index - 1) / 2;
    }

    return NULL_INDEX;
  }

  /**
   * Returns the index of the left child.
   *
   * @param index index of the element
   * @return index of the left child
   */
  private int getLeftChild(int index) {
    return index * 2 + 1;
  }

  /**
   * Returns the index of the right child.
   *
   * @param index index of the element
   * @return index of the right child
   */
  private int getRightChild(int index) {
    return index * 2 + 2;
  }

  /**
   * Returns the index of the grand parents.
   *
   * @param index index of the element
   * @return index of the grand parents
   */
  private int getGrandParents(int index) {
    if (hasGrandParents(index)) {
      return (index - 3) / 4;
    }
    return NULL_INDEX;
  }

  /**
   * Checks if the element has parents.
   *
   * @param index index of the element
   * @return <code>true</code> if the element has parents
   */
  private boolean hasParents(int index) {
    return index > 0;
  }

  /**
   * Checks if the element has grand parents.
   *
   * @param index index of the element
   * @return <code>true</code> if the element has grand parents
   */
  private boolean hasGrandParents(int index) {
    return index > 2;
  }

  /**
   * Checks if the element has children.
   *
   * @param index index of the element
   * @return <code>true</code> if the element has children
   */
  private boolean hasChildren(int index) {
    return getLeftChild(index) < heap.size();
  }

  /**
   * Checks if the element has grand children.
   *
   * @param index index of the element
   * @return <code>true</code> if the element has grand children
   */
  private boolean hasGrandChildren(int index) {
    return getLeftChild(getLeftChild(index)) < heap.size();
  }

  /**
   * Checks if an element is in a max level.
   *
   * @param index the index of the element
   * @return <code>true</code> if it's a max level
   */
  private boolean isMaxLevel(int index) {
    return (int) (Math.log(index + 1) / Math.log(2)) % 2 == 1;
  }

  /**
   * For debugging purposes
   */
  public void test() {
    for (int i = 0; i < heap.size(); i++) {
      HeapNode<K, V> node = heap.get(i);
      Integer index = indices.get(node.getValue());
      if (index == null) {
        System.out.println("Node " + node);
        System.out.println("index " + i + " != indices " + index);
        System.out.println(this);
      }

      if (index != i) {
        System.out.println("Node " + node);
        System.out.println("index " + i + " != indices " + index);
        System.exit(1);
      }
    }
  }
}
