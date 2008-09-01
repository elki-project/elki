package de.lmu.ifi.dbs.elki.converter;


/**
 * A WekaAttribute - an implementing class may be either
 * a nominal, numeric, or string attribute.
 *
 * @author Arthur Zimek
 */
public interface WekaAttribute extends Comparable<WekaAttribute> {
    /**
     * Key word for a nominal attribute.
     */
    public static final String NOMINAL = "nominal";

    /**
     * Key word for a numeric attribute.
     */
    public static final String NUMERIC = "numeric";

    /**
     * Key word for a string attribute.
     */
    public static final String STRING = "string";


    /**
     * Returns the type of the attribute.
     *
     * @return the type of the attribute, i.e. nominal, numeric, or string
     */
    public String getType();

    /**
     * Returns the value as String representation.
     *
     * @return a representation of the attribute value as String
     */
    public String getValue();

    /**
     * Returns whether the attribute is weka nominal.
     *
     * @return true if the attribute is nominal, false otherwise
     */
    public boolean isNominal();

    /**
     * Returns whether the attribute is weka string.
     *
     * @return true if the attribute is string, false otherwise
     */
    public boolean isString();

    /**
     * Returns whether the attribute is weka numeric.
     *
     * @return true if the attribute is numeric, false otherwise
     */
    public boolean isNumeric();

    /**
     * To attributes are considered to be equal,
     * if they are of the same type
     * and the comparison by compareTo
     * results in 0.
     *
     * @param o another object to test for equality
     * @return true if o is of the same type as this
     *         and <code>this.compareTo((W)o)==0</code>.
     */
    public boolean equals(Object o);

}
