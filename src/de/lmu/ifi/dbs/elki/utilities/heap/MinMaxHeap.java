package de.lmu.ifi.dbs.elki.utilities.heap;

import de.lmu.ifi.dbs.elki.utilities.Identifiable;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A double-ended priority queue implemented as a binary heap. This heap
 * provides access to the minimum and the maximums elements in the queue.
 * Elements stored in this heap must be instances of  <code>HeapNode<\code>
 * (@see HeapNode).
 *
 * @author Elke Achtert 
 */
public class MinMaxHeap<K extends Comparable<K>, V extends Identifiable<?>> implements Heap<K, V> {
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
  Hashtable<Integer, Integer> indices;

  /**
   * Constructs and initialises a new min-max-heap.
   */
  public MinMaxHeap() {
    this.heap = new Vector<HeapNode<K, V>>();
    this.indices = new Hashtable<Integer, Integer>();
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public void addNode(HeapNode<K, V> node) {
    if (indices.containsKey(node.getValue().getID()))
      throw new IllegalArgumentException("Node " + node + " already exists in this heap!");

    heap.add(node);
    indices.put(node.getValue().getID(), heap.size() - 1);

    bubbleUp();
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
    indices.remove(min.getValue().getID());

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
    int maxIndex = hasChildren(0) ? getGreatestChild(0) : 0;
    swap(maxIndex, lastIndex);
    HeapNode<K, V> max = heap.get(lastIndex);
    heap.remove(lastIndex);

    // actualize indices
    indices.remove(max.getValue().getID());

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
    int maxIndex = hasChildren(0) ? getGreatestChild(0) : 0;
    return getNodeAt(maxIndex);
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
    return indices.get(value.getID());
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
    indices.put(first.getValue().getID(), secondIndex);
    indices.put(second.getValue().getID(), firstIndex);
  }

  /**
   * Reestablishes the min-max ordering after insertion of a new element.
   */
  private void bubbleUp() {
    int lastIndex = heap.size() - 1;

    // min level
    if (! isMaxLevel(lastIndex)) {
      if (hasParents(lastIndex) &&
          getNodeAt(lastIndex).compareTo(getNodeAt(getParent(lastIndex))) > 0) {
        swap(lastIndex, getParent(lastIndex));
        bubbleUpMax(getParent(lastIndex));
      }
      else {
        bubbleUpMin(lastIndex);
      }

    }
    // max level
    else {
      if (hasParents(lastIndex) &&
          getNodeAt(lastIndex).compareTo(getNodeAt(getParent(lastIndex))) < 0) {
        swap(lastIndex, getParent(lastIndex));
        bubbleUpMin(getParent(lastIndex));
      }
      else {
        bubbleUpMax(lastIndex);
      }
    }
  }

  /**
   * Bubbles up a minimum.
   *
   * @param index index of the element to bubble up
   */
  private void bubbleUpMin(int index) {
    if (hasGrandParent(index)) {
      if (getNodeAt(index).compareTo(getNodeAt(getGrandParent(index))) < 0) {
        swap(index, getGrandParent(index));
        bubbleUpMin(getGrandParent(index));
      }
    }
  }

  /**
   * Bubbles up a maximum.
   *
   * @param index index of the element to bubble up
   */
  private void bubbleUpMax(int index) {
    if (hasGrandParent(index)) {
      if (getNodeAt(index).compareTo(getNodeAt(getGrandParent(index))) > 0) {
        swap(index, getGrandParent(index));
        bubbleUpMax(getGrandParent(index));
      }
    }
  }

  /**
   * The trickle down method.
   *
   * @param index index of the element to trickle down
   */
  private void trickleDown(int index) {
    // max level
    if (isMaxLevel(index)) {
      trickleDownMax(index);
    }
    // min level
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
    if (hasChildren(index)) {
      int m = getSmallestChildAndGrandChild(index);

      int c1 = getLeftChild(index);
      int c2 = getRightChild(index);

      // m is a grand child
      if (m != c1 && m != c2) {
        if (getNodeAt(m).compareTo(getNodeAt(index)) < 0) {
          swap(index, m);
          if (getNodeAt(m).compareTo(getNodeAt(getParent(m))) > 0) {
            swap(m, getParent(m));
          }
          trickleDownMin(m);
        }
      }
      // m is a child if index
      else {
        if (getNodeAt(m).compareTo(getNodeAt(index)) < 0) {
          swap(index, m);
        }
      }
    }
  }

  /**
   * Trickles down a maximum.
   *
   * @param index index of the element to trickle down
   */
  private void trickleDownMax(int index) {
    if (hasChildren(index)) {
      int m = getGreatestChildAndGrandChild(index);

      int c1 = getLeftChild(index);
      int c2 = getRightChild(index);

      // m is a grand child
      if (m != c1 && m != c2) {
        if (getNodeAt(m).compareTo(getNodeAt(index)) > 0) {
          swap(index, m);
          if (getNodeAt(m).compareTo(getNodeAt(getParent(m))) < 0) {
            swap(m, getParent(m));
          }
          trickleDownMax(m);
        }
      }
      // m is a child if index
      else {
        if (getNodeAt(m).compareTo(getNodeAt(index)) > 0) {
          swap(index, m);
        }
      }
    }
  }

  /**
   * Returns the index of the smaller child.
   *
   * @param index index of the element
   * @return index of the smaller child
   */
  private int getSmallestChild(int index) {
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

    int smallerLeftIndex = getSmallestChild(getLeftChild(index));
    int smallerRightIndex = hasChildren(getRightChild(index)) ?
                            getSmallestChild(getRightChild(index)) : smallerLeftIndex;

    HeapNode<K, V> smallerLeft = getNodeAt(smallerLeftIndex);
    HeapNode<K, V> smallerRight = getNodeAt(smallerRightIndex);

    if (smallerLeft == null || smallerLeft.compareTo(smallerRight) < 0) {
      return smallerLeftIndex;
    }

    return smallerRightIndex;
  }

  /**
   * Returns the index of the smallest of the children and grand children.
   *
   * @param index index of the element
   * @return index of the smallest of the children and grand children
   */
  private int getSmallestChildAndGrandChild(int index) {
    assert hasChildren(index);

    int smallestChild = getSmallestChild(index);
    if (! hasGrandChildren(index)) return smallestChild;

    int smallestGrandChild = getSmallestGrandChild(index);

    if (getNodeAt(smallestChild).compareTo(getNodeAt(smallestGrandChild)) < 0)
      return smallestChild;
    else return smallestGrandChild;
  }

  /**
   * Returns the index of the greater child.
   *
   * @param index index of the element
   * @return index of the greater child
   */
  private int getGreatestChild(int index) {

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
    int greaterLeftIndex = getGreatestChild(getLeftChild(index));
    int greaterRightIndex = hasChildren(getRightChild(index)) ?
                            getGreatestChild(getRightChild(index)) : greaterLeftIndex;

    HeapNode<K, V> greaterLeft = getNodeAt(greaterLeftIndex);
    HeapNode<K, V> greaterRight = getNodeAt(greaterRightIndex);

    if (greaterLeft == null || greaterLeft.compareTo(greaterRight) > 0) {
      return greaterLeftIndex;
    }

    return greaterRightIndex;
  }

  /**
   * Returns the index of the greatest of the children and grand children.
   *
   * @param index index of the element
   * @return index of the greatest of the children and grand children
   */
  private int getGreatestChildAndGrandChild(int index) {
    assert hasChildren(index);

    int greatestChild = getGreatestChild(index);
    if (! hasGrandChildren(index)) return greatestChild;

    int greatestGrandChild = getGreatestGrandChild(index);

    if (getNodeAt(greatestChild).compareTo(getNodeAt(greatestGrandChild)) > 0)
      return greatestChild;
    else return greatestGrandChild;
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
  private int getGrandParent(int index) {
    if (hasGrandParent(index)) {
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
   * Checks if the element has a grand parent.
   *
   * @param index index of the element
   * @return <code>true</code> if the element has a grand parent
   */
  private boolean hasGrandParent(int index) {
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
   * Checks if the specified index is in a max level.
   *
   * @param index the index to be tested
   * @return <code>true</code> if the specified index is in a max level
   */
  private boolean isMaxLevel(int index) {
    return ((int) Math.floor(Math.log(index + 1) / Math.log(2))) % 2 == 1;
  }
}
