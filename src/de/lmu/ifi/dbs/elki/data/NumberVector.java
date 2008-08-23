package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.converter.WekaNumericAttribute;
import de.lmu.ifi.dbs.elki.converter.WekaObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * NumberVector is an abstract implementation of FeatureVector. Provided is an
 * attribute separator (space), and the ID-methods as required for a
 * DatabaseObject. The equals-method is implemented dynamically for all
 * subclasses to satisfy the requirements as defined in
 * {@link DatabaseObject#equals(Object) DatabaseObject.equals(Object)}. It
 * needs not to be overwritten except for sake of efficiency.
 *
 * @author Arthur Zimek
 * @param <V> the concrete type of this NumberVector
 * @param <N> the type of number, this NumberVector consists of (i.e., a NumberVector {@code v} of type {@code V}
 * and dimensionality {@code d} is an element of {@code N}<sup>{@code d}</sup>)
 */
public abstract class NumberVector<V extends NumberVector<V, N>, N extends Number> extends AbstractDatabaseObject implements FeatureVector<V, N>, WekaObject<WekaNumericAttribute> {

    /**
     * The String to separate attribute values in a String that represents the
     * values.
     */
    public final static String ATTRIBUTE_SEPARATOR = " ";

    @SuppressWarnings("unchecked")
    public V newInstance(N[] values) throws SecurityException,
                                            NoSuchMethodException, IllegalArgumentException,
                                            InstantiationException, IllegalAccessException,
                                            InvocationTargetException {
        Class<?>[] parameterClasses = {values.getClass()};
        Object[] parameterValues = {values};
        Constructor<V> c = (Constructor<V>) this.getClass().getConstructor(parameterClasses);
        return c.newInstance(parameterValues);
    }

    /**
     * An Object obj is equal to this NumberVector if it is an instance of the
     * same runtime class and is of the identical dimensionality and the values
     * of this NumberVector are equal to the values of obj in all dimensions,
     * respectively.
     *
     * @param obj another Object
     * @return true if the specified Object is an instance of the same runtime
     *         class and is of the identical dimensionality and the values of
     *         this NumberVector are equal to the values of obj in all
     *         dimensions, respectively
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this.getClass().isInstance(obj)) {
            V rv = (V) obj;
            boolean equal = (this.getDimensionality() == rv.getDimensionality());
            for (int i = 1; i <= getDimensionality() && equal; i++) {
                // noinspection ConstantConditions
                equal &= this.getValue(i).equals(rv.getValue(i));
            }
            return equal;
        }
        else {
            return false;
        }
    }

    /**
     * Returns the attributes as array of WekaNumericAttributes.
     *
     * @return the attributes as array of WekaNumericAttributes
     */
    public WekaNumericAttribute[] getAttributes() {
        WekaNumericAttribute[] attributes = new WekaNumericAttribute[this.getDimensionality()];
        for (int d = 1; d <= this.getDimensionality(); d++) {
            attributes[d - 1] = new WekaNumericAttribute(this.getValue(d).doubleValue());
        }
        return attributes;
    }

    /**
     * @return the value at the specified dimension
     * @see #getValue(int)
     */
    public double getMin(int dimension) {
        return getValue(dimension).doubleValue();
    }

    /**
     * @return the value at the specified dimension
     * @see #getValue(int)
     */
    public double getMax(int dimension) {
        return getValue(dimension).doubleValue();
    }

}
