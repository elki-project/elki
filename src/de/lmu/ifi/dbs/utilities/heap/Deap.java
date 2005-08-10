package de.lmu.ifi.dbs.utilities.heap;

/**
 * Subclass of a MinMaxHeap that can be an entry in a persistent heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class Deap extends MinMaxHeap {

  /**
   * The index of this deap in the persistent heap.
   */
  private int index;

  /**
   * The index of this deap in the cachePath of the persistent heap.
   */
  private int cacheIndex;

  /**
   * Creates a new Deap with the specified parameters.
   *
   * @param length     the final length of the deap
   * @param index      the index of this deap in the persistent heap
   * @param cacheIndex the index of this deap in the cachePath of the persistent heap
   */
  public Deap(final int length, int index, int cacheIndex) {
    super(length, false);
    this.index = index;
    this.cacheIndex = cacheIndex;
  }

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  public synchronized void addNode(final HeapNode node) {
    if (! (node instanceof PersistentHeapNode))
      throw new IllegalArgumentException("Node has to be instance of PersistentHeapNode!");

    PersistentHeapNode n = (PersistentHeapNode) node;
    super.addNode(n);
    n.setPersistentHeapIndex(index);
  }

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  public HeapNode getMinNode() {
    PersistentHeapNode min = (PersistentHeapNode) super.getMinNode();
    min.setPersistentHeapIndex(-1);
    return min;
  }

  /**
   * Retrieves and removes the maximum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return The maximum node of this heap, null in case of emptyness.
   */
  public synchronized HeapNode getMaxNode() {
    PersistentHeapNode max = (PersistentHeapNode) super.getMaxNode();
    max.setPersistentHeapIndex(-1);
    return max;
  }

  /**
   * Returns the index of this deap in the persistent heap.
   *
   * @return the index of this deap in the persistent heap
   */
  public int getIndex() {
    return index;
  }

  /**
   * Returns the index of this deap in the cachePath of the persistent heap.
   *
   * @return the index of this deap in the cachePath of the persistent heap
   */
  public int getCacheIndex() {
    return cacheIndex;
  }

  /**
   * Sets the index of this deap in the persistent heap.
   *
   * @param index the index to be set
   */
  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * Sets the index of this deap in the cachePath of the persistent heap.
   *
   * @param cacheIndex the cache index to be set
   */
  public void setCacheIndex(int cacheIndex) {
    this.cacheIndex = cacheIndex;
  }

  /**
   * Returns true if this deap is full, false otherwise.
   *
   * @return true if this deap is full, false otherwise
   */
  public boolean isFull() {
    return this.lastHeap + 1 == length;
  }

  /**
   * Moves all elements from this deap into the specified deap.
   *
   * @param other the deap to move all elements to
   */
  public void moveAll(Deap other) {
    other.array = this.array;
    other.lastHeap = this.lastHeap;

    for (int i = 0; i <= other.lastHeap; i++) {
      PersistentHeapNode node = (PersistentHeapNode) other.array[i];
      node.setPersistentHeapIndex(other.index);
    }

    this.array = new HeapNode[length];
    this.lastHeap = nullIndex;
  }
}
