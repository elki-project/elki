package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.util.Iterator;
import java.util.List;

/**
 * A RealVector is to store real values approximately as double values.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class RealVector implements FeatureVector {
  /**
   * The String to separate attribute values in a String that represents the values.
   */
  public final static String ATTRIBUTE_SEPARATOR = " ";

  /**
   * Keeps the values of the real vector
   */
  private double[] values;

  /**
   * The unique id of this object.
   */
  private Integer id;

  /**
   * Provides a feature vector consisting of double values
   * according to the given Double values.
   *
   * @param values the values to be set as values of the real vector
   */
  public RealVector(List<Double> values) {
    int i = 0;
    this.values = new double[values.size()];
    for (Iterator<Double> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides a real vector consisting of the given double values.
   *
   * @param values the values to be set as values of the real vector
   */
  public RealVector(double[] values) {
    this.values = new double[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Expects a matrix of one column.
   *
   * @param columnMatrix a matrix of one column
   */
  public RealVector(Matrix columnMatrix) {
    values = new double[columnMatrix.getRowDimension()];
    for (int i = 0; i < values.length; i++) {
      values[i] = columnMatrix.get(i, 0);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#getDimensionality()
   */
  public int getDimensionality() {
    return values.length;
  }

  /**
   * Returns a clone of the values of this RealVector.
   *
   * @return a clone of the values of this RealVector
   */
  public double[] getValues() {
    return (double[]) values.clone();
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#getValue(int)
   */
  public double getValue(int dimension) {
    if (dimension < 1 || dimension > values.length) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
    return values[dimension - 1];
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#getVector()
   */
  public Matrix getVector() {
    return new Matrix(values, values.length);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#plus(de.lmu.ifi.dbs.data.FeatureVector)
   */
  public FeatureVector plus(FeatureVector rv) {
    if (rv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + rv.getDimensionality() + ".");
    }
    double[] values = new double[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + rv.getValue(i + 1);
    }
    return new RealVector(values);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#nullVector()
   */
  public FeatureVector nullVector() {
    return new RealVector(new double[this.values.length]);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#negativeVector()
   */
  public FeatureVector negativeVector() {
    return multiplicate(-1);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#multiplicate(double)
   */
  public FeatureVector multiplicate(double k) {
    double[] values = new double[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.values[i] * -1;
    }
    return new RealVector(values);
  }

  /**
   * Returns the unique id of this RealVector object.
   *
   * @return the unique id of this RealVector object
   */
  public int getID() {
    return id;
  }

  /**
   * Sets the id of this RealVector object.
   * The id must be unique within one database.
   *
   * @param id the id to be set
   */
  public void setID(int id) {
    this.id = id;
  }

  /**
   * Provides a deep copy of this object.
   *
   * @return a copy of this object
   */
  public MetricalObject copy() {
    return new RealVector((double[]) this.values.clone());
  }

  /**
   * @see FeatureVector#toString()
   */
  @Override
  public String toString() {
    StringBuffer featureLine = new StringBuffer();
    for (int i = 0; i < values.length; i++) {
      featureLine.append(values[i]);
      if (i + 1 < values.length) {
        featureLine.append(ATTRIBUTE_SEPARATOR);
      }
    }
    return featureLine.toString();
  }

}
