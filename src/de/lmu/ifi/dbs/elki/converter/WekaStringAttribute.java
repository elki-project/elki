package de.lmu.ifi.dbs.elki.converter;

/**
 * A string attribute.
 *
 * @author Arthur Zimek
 */
public class WekaStringAttribute extends WekaAbstractAttribute<WekaStringAttribute> {
    /**
     * Holds the value.
     */
    private String value;

    /**
     * Sets the given value as a string value.
     *
     * @param value the value of this attribute
     */
    public WekaStringAttribute(String value) {
        super(STRING);
        this.value = value;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.converter.WekaAttribute#getValue()
     */
    public String getValue() {
        return value;
    }

    /**
     * Two string attributes are compared by their values.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(WekaStringAttribute o) {
        return this.value.compareTo(o.value);
    }


}
