package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A NumberVector is to store real values approximately as double values.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DoubleVector extends RealVector<Double> {

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
    values = new double[columnMatrix.getRowDimension()];
    for (int i = 0; i < values.length; i++) {
      values[i] = columnMatrix.get(i, 0);
    }
  }

  /**
   * @see RealVector#newInstance(double[])
   * @return a new DoubleVector with the specified values
   */
  public RealVector<Double> newInstance(double[] values) {
    return new DoubleVector(values);
  }

  /**
   * Returns a new DoubleVector with values between 0 and 1.
   *
   * @see FeatureVector#randomInstance(Random)
   */
  public FeatureVector<Double> randomInstance(Random random) {
    double[] randomValues = new double[getDimensionality()];
    for (int i = 0; i < randomValues.length; i++) {
      //int multiplier = random.nextBoolean() ? 1 : -1;
      //randomValues[i] = random.nextDouble() * Double.MAX_VALUE * multiplier;
      randomValues[i] = random.nextDouble();
    }
    return new DoubleVector(randomValues);
  }

  /**
   * @see FeatureVector#randomInstance(Number, Number, java.util.Random)
   */
  public FeatureVector<Double> randomInstance(Double min, Double max, Random random) {
    double[] randomValues = new double[getDimensionality()];
    for (int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextDouble() * (max - min) + min;
    }
    return new DoubleVector(randomValues);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#getDimensionality()
   */
  public int getDimensionality() {
    return values.length;
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#getValue(int)
   */
  public Double getValue(int dimension) {
    if (dimension < 1 || dimension > values.length) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
    return values[dimension - 1];
  }

  /**
   * @see FeatureVector#getColumnVector()
   */
  public Matrix getColumnVector() {
    return new Matrix(values, values.length);
  }

  /**
   * @see FeatureVector#getRowVector()
   */
  public Matrix getRowVector() {
    return new Matrix(new double[][]{values.clone()});
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#plus(de.lmu.ifi.dbs.data.FeatureVector)
   */
  public FeatureVector<Double> plus(FeatureVector<Double> fv) {
    if (fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    double[] values = new double[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.getValue(i + 1);
    }
    return new DoubleVector(values);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#nullVector()
   */
  public FeatureVector<Double> nullVector() {
    return new DoubleVector(new double[this.values.length]);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#negativeVector()
   */
  public FeatureVector<Double> negativeVector() {
    return multiplicate(-1);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#multiplicate(double)
   */
  public FeatureVector<Double> multiplicate(double k) {
    double[] values = new double[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.values[i] * k;
    }
    return new DoubleVector(values);
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
