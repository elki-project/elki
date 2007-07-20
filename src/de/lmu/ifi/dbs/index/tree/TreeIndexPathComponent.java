package de.lmu.ifi.dbs.index.tree;

/**
 * Represents a component in an IndexPath. A component in an IndexPath consists
 * of the entry of the index (representing a node or a data object) and the index
 * of the component in its parent.
 *
 * @author Elke Achtert 
 */
public class TreeIndexPathComponent<E extends Entry> {
  /**
   * The entry of the component.
   */
  private E entry;

  /**
   * The index of the component in its parent.
   */
  private Integer index;

  /**
   * Creates a new IndexPathComponent.
   *
   * @param entry the entry of the component
   * @param index index of the component in its parent
   */
  public TreeIndexPathComponent(E entry, Integer index) {
    this.entry = entry;
    this.index = index;
  }

  /**
   * Returns the entry of the component.
   *
   * @return the entry of the component
   */
  public E getEntry() {
    return entry;
  }

  /**
   * Returns the index of the component in its parent.
   *
   * @return the index of the component in its parent
   */
  public Integer getIndex() {
    return index;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare
   * @return <code>true</code> if the identifier of this component equals
   *         the identifier of the o argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final TreeIndexPathComponent<E> that = (TreeIndexPathComponent<E>) o;
    return (entry.equals(that.entry));
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  public int hashCode() {
    return entry.hashCode();
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return entry.toString() + " ["+index+"]";
  }
}
