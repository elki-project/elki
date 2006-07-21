package de.lmu.ifi.dbs.data;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * A FloatVector is to store real values approximately as float values.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FloatVector extends RealVector<Float> {

  /**
   * Keeps the values of the float vector
   */
  private float[] values;

  /**
   * Provides a FloatVector consisting of float values according to the
   * given Float values.
   *
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(List<Float> values) {
    int i = 0;
    this.values = new float[values.size()];
    for (Iterator<Float> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides a FloatVector consisting of the given float values.
   *
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(float[] values) {
    this.values = new float[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Provides a FloatVector consisting of the given float values.
   *
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(Float[] values) {
    this.values = new float[values.length];
    for (int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * @return a new DoubleVector with the specified values
   * @see RealVector#newInstance(double[])
   */
  public RealVector<Float> newInstance(double[] values) {
    return new FloatVector(Util.convertToFloat(values));
  }

  /**
   * Returns a new FloatVector with values between 0 and 1.
   *
   * @see de.lmu.ifi.dbs.data.FeatureVector#randomInstance(java.util.Random)
   */
  public FeatureVector<Float> randomInstance(Random random) {
    float[] randomValues = new float[getDimensionality()];
    for (int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextFloat();
    }
    return new FloatVector(randomValues);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#randomInstance(Number, Number, java.util.Random)
   */
  public FeatureVector<Float> randomInstance(Float min, Float max, Random random) {
    float[] randomValues = new float[getDimensionality()];
    for (int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextFloat() * (max - min) + min;
    }
    return new FloatVector(randomValues);
  }

  /**
   * @see FeatureVector#getDimensionality()
   */
  public int getDimensionality() {
    return values.length;
  }

  /**
   * @see FeatureVector#getValue(int)
   */
  public Float getValue(int dimension) {
    if (dimension < 1 || dimension > values.length) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
    return values[dimension - 1];
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#getColumnVector()
   */
  public Matrix getColumnVector() {
    return new Matrix(Util.convertToDoubles(values), values.length);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#getRowVector()
   */
  public Matrix getRowVector() {
    return new Matrix(new double[][]{Util.convertToDoubles(values)});
  }

  /**
   * @see FeatureVector#plus(FeatureVector)
   */
  public FeatureVector<Float> plus(FeatureVector<Float> fv) {
    if (fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    float[] values = new float[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.getValue(i + 1);
    }
    return new FloatVector(values);
  }

  /**
   * @see FeatureVector#nullVector()
   */
  public FeatureVector<Float> nullVector() {
    return new FloatVector(new float[this.values.length]);
  }

  /**
   * @see FeatureVector#negativeVector()
   */
  public FeatureVector<Float> negativeVector() {
    return multiplicate(-1);
  }

  /**
   * @see FeatureVector#multiplicate(double)
   */
  public FeatureVector<Float> multiplicate(double k) {
    float[] values = new float[this.values.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = (float) (this.values[i] * k);
    }
    return new FloatVector(values);
  }

  /**
   * @see de.lmu.ifi.dbs.data.FeatureVector#toString()
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
