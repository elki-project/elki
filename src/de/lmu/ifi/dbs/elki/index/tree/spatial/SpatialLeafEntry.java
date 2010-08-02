package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.AbstractLeafEntry;

/**
 * Represents an entry in a leaf node of a spatial index.
 * A SpatialLeafEntry consists of an id (representing the unique id of the
 * underlying data object) and the values of the underlying data object.
 *
 * @author Elke Achtert 
 */
public class SpatialLeafEntry extends AbstractLeafEntry implements SpatialEntry {
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
  public SpatialLeafEntry(DBID id, double[] values) {
    super(id);
    this.values = values;
  }

  /**
   * @return a MBR consisting of the values array
   */
  @Override
  public HyperBoundingBox getMBR() {
    return new HyperBoundingBox(values, values);
  }

  /**
   * Throws an UnsupportedOperationException
   *
   * @throws UnsupportedOperationException thrown, since this is a leaf
   */
  @Override
  public void setMBR(@SuppressWarnings("unused") HyperBoundingBox mbr) {
    throw new UnsupportedOperationException("This entry is a leaf entry!");
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  /**
   * @return the value at the specified dimension
   */
  @Override
  public double getMin(int dimension) {
    return values[dimension - 1];
  }

  /**
   * @return the value at the specified dimension
   */
  @Override
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
    out.writeInt(values.length);
    for(double v : values) {
      out.writeDouble(v);
    }
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
    values = new double[in.readInt()];
    for(int d = 0; d<values.length;d++) {
      values[d] = in.readDouble();
    }
  }
}