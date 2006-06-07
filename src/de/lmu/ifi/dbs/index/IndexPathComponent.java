package de.lmu.ifi.dbs.index;

/**
 * Represents a component in an IndexPath. A component in an IndexPath consists
 * of the entry of the index (representing a node or a data object) and the index
 * of the component in its parent.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class IndexPathComponent {
  /**
   * The entry of the component.
   */
  private Entry entry;

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
  public IndexPathComponent(Entry entry, Integer index) {
    this.entry = entry;
    this.index = index;
  }

  /**
   * Returns the entry of the component.
   *
   * @return the entry of the component
   */
  public Entry getEntry() {
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

    final IndexPathComponent that = (IndexPathComponent) o;
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
}
