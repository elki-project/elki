package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.index.tree.AbstractEntry;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a leaf node of a spatial index.
 * A SpatialLeafEntry consists of an id (representing the unique id of the
 * underlying data object) and the values of the underlying data object.
 *
 * @author Elke Achtert 
 */
public class SpatialLeafEntry extends AbstractEntry implements SpatialEntry {
  private static final long serialVersionUID = 1;

  /**
   * The values of the underlying data object.
   */
  private double[] values;

  /**
   * Empty constructor for serialization purposes.
   */
  public SpatialLeafEntry() {
    super();
  }

  /**
   * Constructs a new LeafEntry object with the given parameters.
   *
   * @param id     the unique id of the underlying data object
   * @param values the values of the underlying data object
   */
  public SpatialLeafEntry(int id, double[] values) {
    super(id);
    this.values = values;
  }

  /**
   * @return true
   */
  public boolean isLeafEntry() {
    return true;
  }

  /**
   * @return a MBR consisting of the values array
   */
  public HyperBoundingBox getMBR() {
    return new HyperBoundingBox(values, values);
  }

  /**
   * Throws an UnsupportedOperationException
   *
   * @throws UnsupportedOperationException
   */
  public void setMBR(HyperBoundingBox mbr) {
    throw new UnsupportedOperationException("This entry is a leaf entry!");
  }

  public int getDimensionality() {
    return values.length;
  }

  /**
   * @return the value at the specified dimension
   */
  public double getMin(int dimension) {
    return values[dimension - 1];
  }

  /**
   * @return the value at the specified dimension
   */
  public double getMax(int dimension) {
    return values[dimension - 1];
  }

  /**
   * Returns the values of the underlying data object of this entry.
   *
   * @return the values of the underlying data object of this entry
   */
  public double[] getValues() {
    return values;
  }

  /**
   * Calls the super method and writes the values of this entry to the specified
   * stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(values);
  }

  /**
   * Calls the super method and reads the values of this entry from the specified
   * input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.values = (double[]) in.readObject();
  }


}
