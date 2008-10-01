package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.index.tree.AbstractEntry;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a directory node of a spatial index.
 * A SpatialDirectoryEntry consists of an id (representing the unique id
 * of the underlying spatial node) and the minmum bounding rectangle
 * of the underlying spatial node.
 *
 * @author Elke Achtert 
 */
public class SpatialDirectoryEntry extends AbstractEntry implements SpatialEntry {
  private static final long serialVersionUID = 1;

  /**
   * The minmum bounding rectangle of the underlying spatial node.
   */
  private HyperBoundingBox mbr;

  /**
   * Empty constructor for serialization purposes.
   */
  public SpatialDirectoryEntry() {
	  // empty constructor
  }

  /**
   * Constructs a new SpatialDirectoryEntry object with the given parameters.
   *
   * @param id  the unique id of the underlying spatial node
   * @param mbr the minmum bounding rectangle of the underlying spatial node
   */
  public SpatialDirectoryEntry(int id, HyperBoundingBox mbr) {
    super(id);
    this.mbr = mbr;
  }

  /**
   * @return false
   */
  public boolean isLeafEntry() {
    return false;
  }

  public HyperBoundingBox getMBR() {
    return mbr;
  }

  public int getDimensionality() {
    return mbr.getDimensionality();
  }

  /**
   * @return the coordinate at the specified dimension of the minimum hyper point of the MBR
   *         of the underlying node
   */
  public double getMin(int dimension) {
    return mbr.getMin(dimension);
  }

  /**
   * @return the coordinate at the specified dimension of the maximum hyper point of the MBR
   *         of the underlying node
   */
  public double getMax(int dimension) {
    return mbr.getMax(dimension);
  }

  /**
   * Sets the MBR of this entry.
   *
   * @param mbr the MBR to be set
   */
  public void setMBR(HyperBoundingBox mbr) {
    this.mbr = mbr;
  }

  /**
   * Calls the super method and writes the MBR object of
   * this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(mbr);
  }

  /**
   * Calls the super method and reads the MBR object of
   * this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.mbr = (HyperBoundingBox) in.readObject();
  }
}
