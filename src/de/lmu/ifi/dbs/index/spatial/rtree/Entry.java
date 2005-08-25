package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.MBR;

import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * Abstract supercalss for an entry in a node of a RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract class Entry implements Externalizable  {
  /**
   * The unique id of the underlying spatial object.
   */
  int id;

  /**
   * Empty constructor for serialization purposes.
   */
  public Entry() {
  }

  /**
   * Constructs a new Entry object with the given parameters.
   *
   * @param id  the unique id of the underlying spatial object
   */
  public Entry(int id) {
    this.id = id;
  }

  /**
   * Returns the id of the underlying spatial object of this entry.
   *
   * @return the id of the underlying spatial object of this entry
   */
  public int getID() {
    return id;
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Entry)) return false;

    final Entry entry = (Entry) o;

    return id == entry.id;
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
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  abstract MBR getMBR();
}

