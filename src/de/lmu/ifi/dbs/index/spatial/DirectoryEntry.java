package de.lmu.ifi.dbs.index.spatial;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * The class DirectoryEntry represents an entry in a directory node of a spatial index.
 * A DirectoryEntry consists of a pair of id (representing the unique id
 * of the underlying spatial object) and the minmum bounding rectangle
 * of the underlying spatial object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DirectoryEntry  extends Entry {
   /**
   * The minmum bounding rectangle of the underlying spatial object.
   */
  private MBR mbr;

  /**
   * Empty constructor for serialization purposes.
   */
  public DirectoryEntry() {
  }

  /**
   * Constructs a new DirectoryEntry object with the given parameters.
   *
   * @param id  the unique id of the underlying spatial object
   * @param mbr the minmum bounding rectangle of the underlying spatial object
   */
  public DirectoryEntry(int id, MBR mbr) {
    super(id);
    this.mbr = mbr;
  }

  /**
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  public MBR getMBR() {
    return mbr;
  }

  /**
   * Sets the MBR of this entry.
   *
   * @param mbr the MBR to be set
   */
  public void setMBR(MBR mbr) {
    this.mbr = mbr;
  }

  /**
   * Returns the id as a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  public String toString() {
    return "" + id + ", mbr " + mbr;
  }

  /**
   * Calls the super and writes the MBR object of
   * this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(mbr);
  }

  /**
   * Calls the super and reads the MBR object of
   * this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.mbr = (MBR) in.readObject();
  }

  /**
   * Returns true if this entry is a leaf entry, false otherwise.
   *
   * @return true if this entry is a leaf entry, false otherwise
   */
  public boolean isLeafEntry() {
    return false;
  }


}
