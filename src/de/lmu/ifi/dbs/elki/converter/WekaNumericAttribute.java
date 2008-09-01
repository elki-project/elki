package de.lmu.ifi.dbs.elki.converter;

/**
 * A numeric attribute.
 *
 * @author Arthur Zimek
 */
public class WekaNumericAttribute extends WekaAbstractAttribute {
    /**
     * Holds the value.
     */
    private Double value;

    /**
     * Sets the given value as numeric value.
     *
     * @param value the value of the attribute
     */
    public WekaNumericAttribute(double value) {
        super(NUMERIC);
        this.value = value;
    }

    public String getValue() {
        return Double.toString(value);
    }

    /**
     * Two numeric attributes are compared by their values.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(WekaAttribute o) {
      WekaNumericAttribute w = (WekaNumericAttribute) o;
      return this.value.compareTo(w.value);
    }

}
