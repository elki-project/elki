package de.lmu.ifi.dbs.elki.distance;

/**
 * Provides a Distance for a number-valued distance.
 * 
 * @author Elke Achtert
 * @param <D> the (final) type of NumberDistance used
 * @param <N> the type of Number used (e.g. Double, Integer, Float, etc.)
 */
public abstract class NumberDistance<D extends NumberDistance<D, N>, N extends Number> extends AbstractDistance<D> {
  /**
   * Constructs a new NumberDistance object that represents the value argument.
   * 
   * @param value the value to be represented by the NumberDistance.
   */
  public NumberDistance() {
    super();
  }

  /**
   * Returns the hash code for this NumberDistance, which is the hash code of
   * its value.
   * 
   * @return the hash code of the value
   */
  @Override
  public final int hashCode() {
    return getValue().hashCode();
  }

  /**
   * Compares this NumberDistance with the given NumberDistance wrt the
   * represented value.
   * <p/>
   * <code>d1.compareTo(d2)</code> is the same as
   * {@link Double#compare(double,double) Double.compare(d1.value.doubleValue(),
   * d2.value.doubleValue())}. Subclasses may need to overwrite this method if
   * necessary.
   * 
   * @return a negative integer, zero, or a positive integer as the value of
   *         this NumberDistance is less than, equal to, or greater than the
   *         value of the specified NumberDistance.
   */
  public int compareTo(D other) {
    return Double.compare(this.doubleValue(), other.doubleValue());
  }

  /**
   * Returns a string representation of this NumberDistance.
   * 
   * @return the value of this NumberDistance.
   */
  @Override
  public final String toString() {
    return getValue().toString();
  }

  /**
   * Returns the value of this NumberDistance.
   * 
   * @return the value of this NumberDistance
   */
  public abstract N getValue();

  /**
   * Sets the value of this NumberDistance.
   * 
   * @param value the value to be set
   */
  abstract void setValue(N value);
  
  /**
   * Get the value as double.
   * 
   * @return same result as getValue().doubleValue() but may be more efficient.
   */
  public abstract double doubleValue();
  
  /**
   * Get the value as float.
   * 
   * @return same result as getValue().floatValue() but may be more efficient.
   */
  public float floatValue() {
    return (float) doubleValue();
  }
  
  /**
   * Get the value as int.
   * 
   * @return same result as getValue().intValue() but may be more efficient.
   */
  public abstract int intValue();
  
  /**
   * Get the value as long.
   * 
   * @return same result as getValue().longValue() but may be more efficient.
   */
  public long longValue() {
    return intValue();
  }
  
  /**
   * Get the value as short.
   * 
   * @return same result as getValue().shortValue() but may be more efficient.
   */
  public short shortValue() {
    return (short) intValue();
  }
  
  /**
   * Get the value as byte.
   * 
   * @return same result as getValue().byteValue() but may be more efficient.
   */
  public byte byteValue() {
    return (byte) intValue();
  }
}
