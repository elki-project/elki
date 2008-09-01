package de.lmu.ifi.dbs.elki.converter;

import de.lmu.ifi.dbs.elki.data.ClassLabel;

/**
 * A nominal attribute.
 * The value is supposed to be a class label.
 *
 * @author Arthur Zimek
 */
public class WekaNominalAttribute<L extends ClassLabel<L>> extends WekaAbstractAttribute {
    /**
     * Holds the value.
     */
    private L value;

    /**
     * Sets the value as a nominal attribute.
     *
     * @param value the value of the attribute
     */
    public WekaNominalAttribute(L value) {
        super(NOMINAL);
        this.value = value;
    }

    public String getValue() {
        return value.toString();
    }

    /**
     * Two nominal attributes are compared by their values.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public int compareTo(WekaAttribute o) {
      WekaNominalAttribute<L> w = (WekaNominalAttribute<L>) o;
      return this.value.compareTo(w.value);
    }

}
