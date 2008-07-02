package de.lmu.ifi.dbs.elki.converter;

import java.util.Arrays;

/**
 * WekaAbstractAttribute handles the type of the attribute.
 * 
 * @author Arthur Zimek
 */
public abstract class WekaAbstractAttribute<W extends WekaAbstractAttribute<W>>
        implements WekaAttribute<W>
{
    /**
     * Collects the possible types of attributes.
     */
    private static final String[] TYPES = { NOMINAL, NUMERIC, STRING };

    /**
     * The index of the type {@link WekaAttribute#NOMINAL NOMINAL} in
     * {@link #TYPES TYPES}.
     */
    private static final int NOMINAL_INDEX = Arrays
            .binarySearch(TYPES, NOMINAL);

    /**
     * The index of the type {@link WekaAttribute#NUMERIC NUMERIC} in
     * {@link #TYPES TYPES}.
     */
    private static final int NUMERIC_INDEX = Arrays
            .binarySearch(TYPES, NUMERIC);

    /**
     * The index of the type {@link WekaAttribute#STRING STRING} in
     * {@link #TYPES TYPES}.
     */
    private static final int STRING_INDEX = Arrays.binarySearch(TYPES, STRING);

    /**
     * Holds the type of this attribute.
     */
    private final int TYPE;

    /**
     * Sets this attribute to the specified type.
     * 
     * @param type
     *            the type of this attribute - one of
     *            {@link WekaAttribute#NOMINAL NOMINAL},
     *            {@link WekaAttribute#NUMERIC NUMERIC}, or
     *            {@link WekaAttribute#STRING STRING}. Another value of the
     *            parameter type will result in an IllegalArgumentException.
     * @throws IllegalArgumentException
     *             if the value of type is none of
     *             {@link WekaAttribute#NOMINAL NOMINAL},
     *             {@link WekaAttribute#NUMERIC NUMERIC}, or
     *             {@link WekaAttribute#STRING STRING}.
     */
    protected WekaAbstractAttribute(String type)
            throws IllegalArgumentException
    {
        TYPE = Arrays.binarySearch(TYPES, type);
        if (TYPE < 0)
        {
            throw new IllegalArgumentException("unknown attribute type: "
                    + type);
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.elki.converter.WekaAttribute#getType()
     */
    public String getType()
    {
        return TYPES[TYPE];
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.elki.converter.WekaAttribute#isNominal()
     */
    public boolean isNominal()
    {
        return TYPE == NOMINAL_INDEX;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.elki.converter.WekaAttribute#isNumeric()
     */
    public boolean isNumeric()
    {
        return TYPE == NUMERIC_INDEX;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.converter.WekaAttribute#isString()
     */
    public boolean isString()
    {
        return TYPE == STRING_INDEX;
    }

    /**
     * Returns the value of the attribute.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getValue();
    }

    /**
     * This equals <code>o</code>, if both are of the same type and
     * <code>this.compareTo((W) o)</code> returns 0.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o)
    {
        try
        {
            W a = (W) o;
            return this.compareTo(a) == 0;

        } catch (ClassCastException e)
        {
            return false;
        }
    }

    /**
     * Returns the hashCode of the attribute value.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return getValue().hashCode();
    }
}
