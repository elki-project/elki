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
     * The value of this distance.
     */
    private N value;

    /**
     * Constructs a new NumberDistance object that represents the value argument.
     *
     * @param value the value to be represented by the NumberDistance.
     */
    public NumberDistance(N value) {
        super();
        this.value = value;
    }

    /**
     * Returns the hash code for this NumberDistance, which is the hash code of its value.
     *
     * @return the hash code of the value
     */
    @Override
    public final int hashCode() {
        return value.hashCode();
    }

    /**
     * Compares this NumberDistance with the given NumberDistance wrt the
     * represented value. <p/>
     * <code>d1.compareTo(d2)</code> is the same as
     * {@link Double#compare(double,double) Double.compare(d1.value.doubleValue(), d2.value.doubleValue())}.
     * Subclasses may need to overwrite this method if necessary.
     *
     * @return a negative integer, zero, or a positive integer as the value of this NumberDistance
     *         is less than, equal to, or greater than the value of the specified NumberDistance.
     */
    public int compareTo(D other) {
        return Double.compare(this.value.doubleValue(), other.value.doubleValue());
    }

    /**
     * Returns a string representation of this NumberDistance.
     *
     * @return the value of this NumberDistance.
     */
    @Override
    public final String toString() {
        return value.toString();
    }

    /**
     * Returns the value of this NumberDistance.
     *
     * @return the value of this NumberDistance
     */
    public final N getValue() {
        return value;
    }

    /**
     * Sets the value of this NumberDistance.
     * @param value the value to be set
     */
    void setValue(N value) {
        this.value = value;
    }
}
