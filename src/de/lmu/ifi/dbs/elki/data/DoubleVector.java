package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A DoubleVector is to store real values approximately as double values.
 *
 * @author Arthur Zimek
 */
public class DoubleVector extends AbstractNumberVector<DoubleVector,Double> {

  /**
   * Keeps the values of the real vector
   */
  private double[] values;

  /**
   * Provides a feature vector consisting of double values according to the
   * given Double values.
   *
   * @param values the values to be set as values of the real vector
   */
  public DoubleVector(List<Double> values) {
    int i = 0;
    this.values = new double[values.size()];
    for (Iterator<Double> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides a DoubleVector consisting of the given double values.
   *
   * @param values the values to be set as values of theDoubleVector
   */
  public DoubleVector(double[] values) {
    this.values = new double[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Provides a DoubleVector consisting of the given double values.
   *
   * @param values the values to be set as values of theDoubleVector
   */
  public DoubleVector(Double[] values) {
    this.values = new double[values.length];
    //noinspection ManualArrayCopy
    for (int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Expects a matrix of one column.
   *
   * @param columnMatrix a matrix of one column
   */
  public DoubleVector(Matrix columnMatrix) {
    values = new double[columnMatrix.getRowDimensionality()];
    for (int i = 0; i < values.length; i++) {
      values[i] = columnMatrix.get(i, 0);
    }
  }

  /**
   * @return a new DoubleVector with the specified values
   */
  @Override
  public DoubleVector newInstance(Vector values) {
    return new DoubleVector(values);
  }
  
  /**
   * @return a new DoubleVector with the specified values
   */
  @Override
  public DoubleVector newInstance(Double[] values) {
    return new DoubleVector(values);
  }
  
  /**
   * @return a new DoubleVector with the specified values
   */
  @Override
  public DoubleVector newInstance(double[] values) {
    return new DoubleVector(values);
  }
  
  

  public DoubleVector newInstance(List<Double> values) {
    return new DoubleVector(values);
  }

  /**
   * Returns a new DoubleVector with random values between 0 and 1.
   *
   */
  public DoubleVector randomInstance(Random random) {
    double[] randomValues = new double[getDimensionality()];
    for (int i = 0; i < randomValues.length; i++) {
      //int multiplier = random.nextBoolean() ? 1 : -1;
      //randomValues[i] = random.nextDouble() * Double.MAX_VALUE * multiplier;
      randomValues[i] = random.nextDouble();
    }
    return new DoubleVector(randomValues);
  }

  public DoubleVector randomInstance(Double min, Double max, Random random) {
    double[] randomValues = new double[getDimensionality()];
    for (int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextDouble() * (max - min) + min;
    }
    return new DoubleVector(randomValues);
  }
  
  /**
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#randomInstance(de.lmu.ifi.dbs.elki.data.NumberVector, de.lmu.ifi.dbs.elki.data.NumberVector, java.util.Random)
   */
  public DoubleVector randomInstance(DoubleVector min, DoubleVector max, Random random) {
    double[] randomValues = new double[getDimensionality()];
    for (int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextDouble() * (max.getValue(i+1) - min.getValue(i+1)) + min.getValue(i+1);
    }
    return new DoubleVector(randomValues);
  }
  
  public int getDimensionality() {
    return values.length;
  }

  /**
   * Returns the value of the specified attribute.
   * 
   * @param dimension the selected attribute. Attributes are counted starting with 1.
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range of the possible attributes
   */
  public Double getValue(int dimension) {
    if (dimension < 1 || dimension > values.length) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
    return values[dimension - 1];
  }

  /**
   * Get a copy of the raw double[] array.
   * 
   * @return copy of values array.
   */
  public double[] getValues() {
    double[] copy = new double[values.length];
    System.arraycopy(values, 0, copy, 0, values.length);
    return copy;
  }

  public Vector getColumnVector() {
    return new Vector(values);
  }

  public Matrix getRowVector() {
    return new Matrix(new double[][]{values.clone()});
  }
  
  public DoubleVector plus(DoubleVector fv) {
    if (fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    double[] values = new double[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.getValue(i + 1);
    }
    return new DoubleVector(values);
  }

  public DoubleVector nullVector() {
    return new DoubleVector(new double[this.values.length]);
  }

  public DoubleVector negativeVector() {
    return multiplicate(-1);
  }

  public DoubleVector multiplicate(double k) {
    double[] values = new double[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.values[i] * k;
    }
    return new DoubleVector(values);
  }

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
