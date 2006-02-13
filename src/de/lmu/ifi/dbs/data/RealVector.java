package de.lmu.ifi.dbs.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * RealVector is an abstract implementation of FeatureVector. Provided is an
 * attribute separator (space), and the ID-methods as required for a
 * MetricalObject. The equals-method is implemented dynamically for all subclasses
 * to satisfy the requirements as defined in {@link DatabaseObject#equals(Object) MetricalObject.equals(Object)}.
 * It needs not to be overwritten except for sake of efficiency.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class RealVector<N extends Number> extends AbstractDatabaseObject<FeatureVector<N>> implements FeatureVector<N> {

  /**
   * The String to separate attribute values in a String that represents the
   * values.
   */
  public final static String ATTRIBUTE_SEPARATOR = " ";

  /**
   * @see FeatureVector#newInstance(Number[])
   */
  public FeatureVector<N> newInstance(N[] values) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Class[] parameterClasses = {values.getClass()};
    Object[] parameterValues = {values};
    Constructor c = this.getClass().getConstructor(parameterClasses);
    return (FeatureVector<N>) c.newInstance(parameterValues);
  }

  /**
   * An Object obj is equal to this RealVector
   * if it is an instance of the same runtime class
   * and is of the identical dimensionality
   * and the values of this RealVector are equal
   * to the values of obj in all dimensions, respectively.
   *
   * @param obj another Object
   * @return true if the specified Object is an instance of the same runtime class
   *         and is of the identical dimensionality
   *         and the values of this RealVector are equal
   *         to the values of obj in all dimensions, respectively
   * @see DatabaseObject#equals(Object)
   */
  public boolean equals(Object obj) {
    if (this.getClass().isInstance(obj)) {
      RealVector rv = (RealVector) obj;
      boolean equal = (this.getDimensionality() == rv.getDimensionality());
      for (int i = 1; i <= getDimensionality() && equal; i++) {
        //noinspection ConstantConditions
        equal &= this.getValue(i).equals(rv.getValue(i));
      }
      return equal;
    }
    else {
      return false;
    }
  }

}
