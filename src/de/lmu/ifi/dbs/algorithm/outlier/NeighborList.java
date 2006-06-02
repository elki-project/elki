package de.lmu.ifi.dbs.algorithm.outlier;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates a list of neighors and implements additionally
 * the Externalizable interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class NeighborList implements Externalizable, Iterable<Neighbor> {
  /**
   * The list containing the neighbors.
   */
  private List<Neighbor> neighbors;

  /**
   * Provides a new neighbor list.
   */
  public NeighborList() {
    neighbors = new ArrayList<Neighbor>();
  }

  /**
   * Provides a neighbor list with the specified initial size.
   *
   * @param size
   */
  public NeighborList(int size) {
    neighbors = new ArrayList<Neighbor>(size);
  }

  /**
   * Returns an iterator over this list.
   *
   * @return an Iterator.
   */
  public Iterator<Neighbor> iterator() {
    return neighbors.iterator();
  }

  /**
   * Appends the specified neighbor to the end of this list.
   *
   * @param neighbor neighbor to be appended to this list
   */
  public void add(Neighbor neighbor) {
    neighbors.add(neighbor);
  }

  /**
   * Inserts the specified neighbor at the specified position in this list.
   * Shifts the neighbor currently at that position
   * and any subsequent element to the right.
   *
   * @param index    index at which the specified neighbor is to be inserted.
   * @param neighbor neighbor to be inserted.
   */
  public void add(int index, Neighbor neighbor) {
    neighbors.add(index, neighbor);
  }


  /**
   * Removes the first occurrence in this list of the specified neighbor.
   *
   * @param neighbor neighbor to be removed from this list, if present.
   */
  public void remove(Neighbor neighbor) {
    neighbors.remove(neighbor);
  }

  /**
   * Removes the neighbor at the specified position in this list.
   *
   * @param index the index of the neigbor to be removed.
   */
  public void remove(int index) {
    neighbors.remove(index);
  }

  /**
   * Returns the neighbor at the specified position in this list.
   *
   * @param index index of neighbor to return.
   * @return the neighbor at the specified position in this list.
   */
  public Neighbor get(int index) {
    return neighbors.get(index);
  }

  /**
   * Returns the number of elements in this list.
   *
   * @return the number of elements in this list
   */
  public int size() {
    return neighbors.size();
  }

  /**
   * Removes and returns the last element in this list.
   *
   * @return the last element in this list
   */
  public Neighbor removeLast() {
    return neighbors.remove(neighbors.size() - 1);
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(neighbors.size());
    for (Neighbor neighbor : neighbors) {
      neighbor.writeExternal(out);
    }
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int size = in.readInt();
    neighbors = new ArrayList<Neighbor>(size);
    for (int i = 0; i < size; i++) {
      Neighbor neighbor = (Neighbor) in.readObject();
      neighbors.add(neighbor);
    }
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return neighbors.toString();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj
   *         argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final NeighborList that = (NeighborList) o;

    ArrayList<Neighbor> thisNeighborsClone = new ArrayList<Neighbor>(neighbors);
    ArrayList<Neighbor> thatNeighborsClone = new ArrayList<Neighbor>(that.neighbors);

    Collections.sort(thisNeighborsClone);
    Collections.sort(thatNeighborsClone);
    return thisNeighborsClone.equals(thatNeighborsClone);
  }

  /**
   * Returns a hash code value for this object.
   *
   * @return a hash code value for this object
   */
  public int hashCode() {
    return neighbors.hashCode();
  }
}
