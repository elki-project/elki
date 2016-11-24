package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Specialized class implementing a one-dimensional double vector without using
 * an array. Saves a little bit of memory, albeit we cannot avoid boxing as long
 * as we want to implement the interface.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class OneDimensionalDoubleVector extends AbstractNumberVector {
  /**
   * Static factory instance.
   */
  public static final OneDimensionalDoubleVector.Factory STATIC = new OneDimensionalDoubleVector.Factory();

  /**
   * The actual data value.
   */
  double val;

  /**
   * Constructor.
   * 
   * @param val Value
   */
  public OneDimensionalDoubleVector(double val) {
    this.val = val;
  }

  @Override
  public int getDimensionality() {
    return 1;
  }

  @Override
  public double doubleValue(int dimension) {
    assert (dimension == 0) : "Non-existant dimension accessed.";
    return val;
  }

  @Override
  public long longValue(int dimension) {
    assert (dimension == 0) : "Non-existant dimension accessed.";
    return (long) val;
  }

  @Override
  public double[] toArray() {
    return new double[] { val };
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has OneDimensionalDoubleVector
   */
  public static class Factory extends AbstractNumberVector.Factory<OneDimensionalDoubleVector> {
    @Override
    public <A> OneDimensionalDoubleVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      assert (adapter.size(array) == 1) : "Incorrect dimensionality for 1-dimensional vector.";
      return new OneDimensionalDoubleVector(adapter.get(array, 0).doubleValue());
    }

    @Override
    public <A> OneDimensionalDoubleVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      assert (adapter.size(array) == 1) : "Incorrect dimensionality for 1-dimensional vector.";
      return new OneDimensionalDoubleVector(adapter.getDouble(array, 0));
    }

    @Override
    public ByteBufferSerializer<OneDimensionalDoubleVector> getDefaultSerializer() {
      // FIXME: add a serializer
      return null;
    }
    
    @Override
    public Class<? super OneDimensionalDoubleVector> getRestrictionClass() {
      return OneDimensionalDoubleVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected OneDimensionalDoubleVector.Factory makeInstance() {
        return STATIC;
      }
    }
  }
}
