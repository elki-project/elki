package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

/**
 * The subspace distance is a special distance that indicates the dissimilarity
 * between subspaces of equal dimensionality. The subspace distance between two
 * points is a pair consisting of the distance between the two subspaces spanned
 * by the strong eigenvectors of the two points and the affine distance between
 * the two subspaces.
 * 
 * @author Elke Achtert
 */
public class SubspaceDistance extends AbstractDistance<SubspaceDistance> {
  /**
   * Serial version number.
   */
  private static final long serialVersionUID = 1;

  /**
   * Indicates a separator.
   */
  public static final String SEPARATOR = "x";

  /**
   * The pattern for parsing subspace values
   */
  public static final Pattern SUBSPACE_PATTERN = Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?" + Pattern.quote(SEPARATOR) + "\\d+(\\.\\d+)?([eE][-]?\\d+)?");

  /**
   * The subspace distance.
   */
  private double subspaceDistance;

  /**
   * The affine distance.
   */
  private double affineDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public SubspaceDistance() {
    // for serialization
  }

  /**
   * Constructs a new SubspaceDistance object consisting of the specified
   * subspace distance and affine distance.
   * 
   * @param subspaceDistance the distance between the two subspaces spanned by
   *        the strong eigenvectors of the two points
   * @param affineDistance the affine distance between the two subspaces
   */
  public SubspaceDistance(double subspaceDistance, double affineDistance) {
    this.subspaceDistance = subspaceDistance;
    this.affineDistance = affineDistance;
  }

  @Override
  public SubspaceDistance plus(SubspaceDistance distance) {
    return new SubspaceDistance(this.subspaceDistance + distance.subspaceDistance, this.affineDistance + distance.affineDistance);
  }

  @Override
  public SubspaceDistance minus(SubspaceDistance distance) {
    return new SubspaceDistance(this.subspaceDistance - distance.subspaceDistance, this.affineDistance - distance.affineDistance);
  }

  /**
   * Returns a string representation of this SubspaceDistance.
   * 
   * @return the values of the subspace distance and the affine distance
   *         separated by blank
   */
  @Override
  public String toString() {
    return Double.toString(subspaceDistance) + " " + Double.toString(affineDistance);
  }

  /**
   * Compares this SubspaceDistance with the given SubspaceDistance wrt the
   * represented subspace distance values. If both values are considered to be
   * equal, the values of the affine distances are compared.
   * 
   * @return the value of {@link Double#compare(double,double)
   *         Double.compare(this.subspaceDistance, other.subspaceDistance)} if
   *         it is a non zero value, the value of
   *         {@link Double#compare(double,double)
   *         Double.compare(this.affineDistance, other.affineDistance)}
   *         otherwise
   */
  @Override
  public int compareTo(SubspaceDistance other) {
    int compare = Double.compare(this.subspaceDistance, other.subspaceDistance);
    if(compare != 0) {
      return compare;
    }
    else {
      return Double.compare(this.affineDistance, other.affineDistance);
    }
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = subspaceDistance != 0.0d ? Double.doubleToLongBits(subspaceDistance) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = affineDistance != 0.0d ? Double.doubleToLongBits(affineDistance) : 0L;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Returns the value of the subspace distance.
   * 
   * @return the value of the subspace distance
   */
  public double getSubspaceDistance() {
    return subspaceDistance;
  }

  /**
   * Returns the value of the affine distance.
   * 
   * @return the value of the affine distance
   */
  public double getAffineDistance() {
    return affineDistance;
  }

  /**
   * Writes the subspace distance value and the affine distance value of this
   * SubspaceDistance to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(subspaceDistance);
    out.writeDouble(affineDistance);
  }

  /**
   * Reads the subspace distance value and the affine distance value of this
   * SubspaceDistance from the specified stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    subspaceDistance = in.readDouble();
    affineDistance = in.readDouble();
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 16 (2 * 8 Byte for two double values)
   */
  @Override
  public int externalizableSize() {
    return 16;
  }

  @Override
  public Pattern getPattern() {
    return SUBSPACE_PATTERN;
  }

  @Override
  public SubspaceDistance parseString(String val) throws IllegalArgumentException {
    if(val.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if(testInputPattern(val)) {
      String[] values = SEPARATOR.split(val);
      return new SubspaceDistance(Double.parseDouble(values[0]), Double.parseDouble(values[1]));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + val + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }

  @Override
  public SubspaceDistance infiniteDistance() {
    return new SubspaceDistance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  @Override
  public SubspaceDistance nullDistance() {
    return new SubspaceDistance(0, 0);
  }

  @Override
  public SubspaceDistance undefinedDistance() {
    return new SubspaceDistance(Double.NaN, Double.NaN);
  }
}