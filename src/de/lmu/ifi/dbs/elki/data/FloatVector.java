package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A FloatVector is to store real values approximately as float values.
 * 
 * @author Elke Achtert
 */
public class FloatVector extends AbstractNumberVector<FloatVector, Float> {

  /**
   * Keeps the values of the float vector
   */
  private float[] values;

  /**
   * Private constructor. NOT for public use.
   */
  private FloatVector(float[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new float[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Provides a FloatVector consisting of float values according to the given
   * Float values.
   * 
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(List<Float> values) {
    int i = 0;
    this.values = new float[values.size()];
    for(Iterator<Float> iter = values.iterator(); iter.hasNext(); i++) {
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
    for(int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Expects a matrix of one column.
   * 
   * @param columnMatrix a matrix of one column
   */
  public FloatVector(Matrix columnMatrix) {
    values = new float[columnMatrix.getRowDimensionality()];
    for(int i = 0; i < values.length; i++) {
      values[i] = (float) columnMatrix.get(i, 0);
    }
  }

  /**
   * @return a new FloatVector with the specified values
   */
  @Override
  public FloatVector newInstance(Vector values) {
    return new FloatVector(values);
  }

  /**
   * @return a new FloatVector with the specified values
   */
  @Override
  public FloatVector newInstance(double[] values) {
    return new FloatVector(Util.convertToFloat(values));
  }

  public FloatVector newInstance(Float[] values) {
    return new FloatVector(values);
  }

  public FloatVector newInstance(List<Float> values) {
    return new FloatVector(values);
  }

  /**
   * @return a new FloatVector with random values between 0 and 1
   */
  public FloatVector randomInstance(Random random) {
    float[] randomValues = new float[getDimensionality()];
    for(int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextFloat();
    }
    return new FloatVector(randomValues, true);
  }

  public FloatVector randomInstance(Float min, Float max, Random random) {
    float[] randomValues = new float[getDimensionality()];
    for(int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextFloat() * (max - min) + min;
    }
    return new FloatVector(randomValues, true);
  }

  /**
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#randomInstance(de.lmu.ifi.dbs.elki.data.NumberVector,
   *      de.lmu.ifi.dbs.elki.data.NumberVector, java.util.Random)
   */
  public FloatVector randomInstance(FloatVector min, FloatVector max, Random random) {
    float[] randomValues = new float[getDimensionality()];
    for(int i = 0; i < randomValues.length; i++) {
      randomValues[i] = random.nextFloat() * (max.getValue(i + 1) - min.getValue(i + 1)) + min.getValue(i + 1);
    }
    return new FloatVector(randomValues, true);
  }

  public int getDimensionality() {
    return values.length;
  }

  public Float getValue(int dimension) {
    if(dimension < 1 || dimension > values.length) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
    return values[dimension - 1];
  }

  public Vector getColumnVector() {
    return new Vector(Util.convertToDoubles(values));
  }

  public Matrix getRowVector() {
    return new Matrix(new double[][] { Util.convertToDoubles(values) });
  }

  public FloatVector plus(FloatVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    float[] values = new float[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.getValue(i + 1);
    }
    return new FloatVector(values, true);
  }

  public FloatVector minus(FloatVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    float[] values = new float[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] - fv.getValue(i + 1);
    }
    return new FloatVector(values, true);
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * FloatVector.
   * 
   * @param f the FloatVector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         FloatVector
   */
  public Float scalarProduct(FloatVector f) {
    if(this.getDimensionality() != f.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + f.getDimensionality() + ".");
    }
    float result = 0.0f;
    for(int i = 0; i < this.getDimensionality(); i++) {
      result += this.values[i] * f.values[i];
    }
    return result;
  }

  public FloatVector nullVector() {
    return new FloatVector(new float[this.values.length], true);
  }

  public FloatVector negativeVector() {
    return multiplicate(-1);
  }

  public FloatVector multiplicate(double k) {
    float[] values = new float[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = (float) (this.values[i] * k);
    }
    return new FloatVector(values, true);
  }

  @Override
  public String toString() {
    StringBuffer featureLine = new StringBuffer();
    for(int i = 0; i < values.length; i++) {
      featureLine.append(values[i]);
      if(i + 1 < values.length) {
        featureLine.append(ATTRIBUTE_SEPARATOR);
      }
    }
    return featureLine.toString();
  }
}
