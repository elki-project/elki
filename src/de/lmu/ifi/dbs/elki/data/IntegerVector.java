package de.lmu.ifi.dbs.elki.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

/**
 * An IntegerVector is to store integer values.
 * 
 * @author Erich Schubert
 */
public class IntegerVector extends AbstractNumberVector<IntegerVector, Integer> implements ByteBufferSerializer<IntegerVector> {
  /**
   * Keeps the values of the real vector
   */
  private int[] values;

  /**
   * Private constructor. NOT for public use.
   */
  private IntegerVector(int[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new int[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Provides a feature vector consisting of int values according to the
   * given Integer values.
   * 
   * @param values the values to be set as values of the integer vector
   */
  public IntegerVector(List<Integer> values) {
    int i = 0;
    this.values = new int[values.size()];
    for(Iterator<Integer> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides an IntegerVector consisting of the given integer values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(int[] values) {
    this.values = new int[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Provides an IntegerVector consisting of the given integer values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(Integer[] values) {
    this.values = new int[values.length];
    for(int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Provides an IntegerVector consisting of the given double values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(double[] values) {
    this.values = new int[values.length];
    for(int i = 0; i < this.values.length; i++) {
      this.values[i] = (int) values[i];
    }
  }

  /**
   * Provides an IntegerVector consisting of the given double vectors values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(Vector values) {
    this(values.getArrayRef());
  }

  /**
   * Returns a new IntegerVector with random values between 0 and 1.
   */
  @Override
  public IntegerVector randomInstance(Random random) {
    int[] randomValues = new int[getDimensionality()];
    for(int i = 0; i < randomValues.length; i++) {
      // int multiplier = random.nextBoolean() ? 1 : -1;
      // randomValues[i] = random.nextDouble() * Double.MAX_VALUE * multiplier;
      randomValues[i] = random.nextInt();
    }
    return new IntegerVector(randomValues, true);
  }

  @Override
  public IntegerVector randomInstance(Integer min, Integer max, Random random) {
    int[] randomValues = new int[getDimensionality()];
    for(int i = 0; i < randomValues.length; i++) {
      randomValues[i] = (int) (random.nextDouble() * (max - min) + min);
    }
    return new IntegerVector(randomValues, true);
  }

  /**
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#randomInstance(de.lmu.ifi.dbs.elki.data.NumberVector,
   *      de.lmu.ifi.dbs.elki.data.NumberVector, java.util.Random)
   */
  @Override
  public IntegerVector randomInstance(IntegerVector min, IntegerVector max, Random random) {
    int[] randomValues = new int[getDimensionality()];
    for(int i = 0; i < randomValues.length; i++) {
      randomValues[i] = (int) (random.nextDouble() * (max.getValue(i + 1) - min.getValue(i + 1)) + min.getValue(i + 1));
    }
    return new IntegerVector(randomValues, true);
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  /**
   * Returns the value of the specified attribute.
   * 
   * @param dimension the selected attribute. Attributes are counted starting
   *        with 1.
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   */
  @Override
  public Integer getValue(int dimension) {
    try {
      return values[dimension - 1];
    }
    catch(IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  /**
   * Returns the value of the specified attribute.
   * 
   * @param dimension the selected attribute. Attributes are counted starting
   *        with 1.
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   */
  @Override
  public double doubleValue(int dimension) {
    try {
      return values[dimension - 1];
    }
    catch(IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  /**
   * Returns the value of the specified attribute as long.
   * 
   * @param dimension the selected attribute. Attributes are counted starting
   *        with 1.
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   */
  @Override
  public long longValue(int dimension) {
    try {
      return values[dimension - 1];
    }
    catch(IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  /**
   * Get a copy of the raw int[] array.
   * 
   * @return copy of values array.
   */
  public int[] getValues() {
    int[] copy = new int[values.length];
    System.arraycopy(values, 0, copy, 0, values.length);
    return copy;
  }

  @Override
  public Vector getColumnVector() {
    double[] data = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      data[i] = values[i];
    }
    return new Vector(data);
  }

  @Override
  public Matrix getRowVector() {
    double[] data = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      data[i] = values[i];
    }
    return new Matrix(new double[][] { data });
  }

  @Override
  public IntegerVector plus(IntegerVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    int[] values = new int[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.values[i];
    }
    return new IntegerVector(values, true);
  }

  @Override
  public IntegerVector minus(IntegerVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    int[] values = new int[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] - fv.values[i];
    }
    return new IntegerVector(values, true);
  }

  @Override
  public IntegerVector nullVector() {
    return new IntegerVector(new int[this.values.length], true);
  }

  @Override
  public IntegerVector negativeVector() {
    return multiplicate(-1);
  }

  @Override
  public IntegerVector multiplicate(double k) {
    int[] values = new int[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = (int) (this.values[i] * k);
    }
    return new IntegerVector(values, true);
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * IntegerVector.
   * 
   * @param d the IntegerVector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         IntegerVector
   */
  @Override
  public Integer scalarProduct(IntegerVector d) {
    if(this.getDimensionality() != d.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + d.getDimensionality() + ".");
    }
    double result = 0.0;
    for(int i = 0; i < this.getDimensionality(); i++) {
      result += this.values[i] * d.values[i];
    }
    return (int) result;
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

  @Override
  public IntegerVector newInstance(Vector values) {
    return new IntegerVector(values);
  }

  @Override
  public IntegerVector newInstance(Integer[] values) {
    return new IntegerVector(values);
  }

  @Override
  public IntegerVector newInstance(double[] values) {
    return new IntegerVector(values);
  }

  @Override
  public IntegerVector newInstance(List<Integer> values) {
    return new IntegerVector(values);
  }

  @Override
  public IntegerVector fromByteBuffer(ByteBuffer buffer) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough data for a double vector!");
    }
    int[] values = new int[dimensionality];
    buffer.asIntBuffer().get(values);
    return new IntegerVector(values, false);
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, IntegerVector vec) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough space for the double vector!");
    }
    buffer.putShort(dimensionality);
    buffer.asIntBuffer().put(vec.values);
  }

  @Override
  public int getByteSize(IntegerVector vec) {
    return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
  }
}