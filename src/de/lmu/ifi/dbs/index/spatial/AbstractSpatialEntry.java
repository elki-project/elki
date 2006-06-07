package de.lmu.ifi.dbs.index.spatial;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Abstract superclass for an entry in a node of a Spatial Index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractSpatialEntry implements SpatialEntry {
  /**
   * The id of the underlying spatial object, this object can be a node or a data object.
   */
  int id;

  /**
   * Empty constructor for serialization purposes.
   */
  public AbstractSpatialEntry() {
  }

  /**
   * Constructs a new Entry object with the given parameters.
   *
   * @param id the unique id of the underlying spatial object
   */
  public AbstractSpatialEntry(int id) {
    this.id = id;
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractSpatialEntry)) return false;

    final SpatialEntry entry = (SpatialEntry) o;

    return id == entry.getID();
  }

  /**
   * @see Object#hashCode()
   */
  public int hashCode() {
    return id;
  }

  /**
   * Writes the id of this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(id);
  }

  /**
   * Reads the id of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.id = in.readInt();
  }

  /**
   * Returns the id of the underlying spatial object.
   *
   * @return the id of the underlying spatial object
   */
  public Integer getID() {
    return id;
  }

  /**
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  abstract public MBR getMBR();

  /**
   * Returns true if this entry is a leaf entry, false otherwise.
   *
   * @return true if this entry is a leaf entry, false otherwise
   */
  abstract public boolean isLeafEntry();
}

