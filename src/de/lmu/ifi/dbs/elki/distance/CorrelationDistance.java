package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The correlation distance is a special Distance that indicates the
 * dissimilarity between correlation connected objects. The correlation distance
 * between two points is a pair consisting of the correlation dimension of two
 * points and the euclidean distance between the two points.
 * 
 * @author Elke Achtert
 * @param <D> distance type
 */
public class CorrelationDistance<D extends CorrelationDistance<D>> extends AbstractDistance<D> {

  /**
   * Generated SerialVersionUID.
   */
  private static final long serialVersionUID = 2829135841596857929L;

  /**
   * The correlation dimension.
   */
  private int correlationValue;

  /**
   * The euclidean distance.
   */
  private double euclideanValue;

  /**
   * Empty constructor for serialization purposes.
   */
  public CorrelationDistance() {
    // for serialization
  }

  /**
   * Constructs a new CorrelationDistance object consisting of the specified
   * correlation value and euclidean value.
   * 
   * @param correlationValue the correlation dimension to be represented by the
   *        CorrelationDistance
   * @param euclideanValue the euclidean distance to be represented by the
   *        CorrelationDistance
   */
  public CorrelationDistance(int correlationValue, double euclideanValue) {
    this.correlationValue = correlationValue;
    this.euclideanValue = euclideanValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  public D plus(D distance) {
    return (D) new CorrelationDistance<D>(this.correlationValue + distance.getCorrelationValue(), this.euclideanValue + distance.getEuclideanValue());
  }

  @SuppressWarnings("unchecked")
  @Override
  public D minus(D distance) {
    return (D) new CorrelationDistance<D>(this.correlationValue - distance.getCorrelationValue(), this.euclideanValue - distance.getEuclideanValue());
  }

  @Override
  public String description() {
    return "CorrelationDistance.correlationValue CorrelationDistance.euclideanValue";
  }

  /**
   * Returns a string representation of this CorrelationDistance.
   * 
   * @return the correlation value and the euclidean value separated by blank
   */
  @Override
  public String toString() {
    return Integer.toString(correlationValue) + " " + Double.toString(euclideanValue);
  }

  /**
   * Compares this CorrelationDistance with the given CorrelationDistance wrt
   * the represented correlation values. If both values are considered to be
   * equal, the euclidean values are compared. Subclasses may need to overwrite
   * this method if necessary.
   * 
   * @return the value of {@link Integer#compareTo(Integer)}
   *         this.correlationValue.compareTo(other.correlationValue)} if it is a
   *         non zero value, the value of {@link Double#compare(double,double)
   *         Double.compare(this.euclideanValue, other.euclideanValue)}
   *         otherwise
   */
  public int compareTo(D other) {
    int compare = new Integer(this.correlationValue).compareTo(other.getCorrelationValue());
    if(compare != 0) {
      return compare;
    }
    else {
      return Double.compare(this.euclideanValue, other.getEuclideanValue());
    }
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = correlationValue;
    temp = euclideanValue != +0.0d ? Double.doubleToLongBits(euclideanValue) : 0l;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final CorrelationDistance<D> other = (CorrelationDistance<D>) obj;
    if (this.correlationValue != other.correlationValue) {
      return false;
    }
    if (this.euclideanValue != other.euclideanValue) {
      return false;
    }
    return true;
  }

  /**
   * Returns the correlation dimension between the objects.
   * 
   * @return the correlation dimension
   */
  public int getCorrelationValue() {
    return correlationValue;
  }

  /**
   * Returns the euclidean distance between the objects.
   * 
   * @return the euclidean distance
   */
  public double getEuclideanValue() {
    return euclideanValue;
  }

  /**
   * Writes the correlation value and the euclidean value of this
   * CorrelationDistance to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(correlationValue);
    out.writeDouble(euclideanValue);
  }

  /**
   * Reads the correlation value and the euclidean value of this
   * CorrelationDistance from the specified stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    correlationValue = in.readInt();
    euclideanValue = in.readDouble();
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 12 (4 Byte for an integer, 8 Byte for a double value)
   */
  @Override
  public int externalizableSize() {
    return 12;
  }

}
