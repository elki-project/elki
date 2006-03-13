package de.lmu.ifi.dbs.converter;

import java.util.Arrays;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class WekaAbstractAttribute<W extends WekaAbstractAttribute<W>> implements WekaAttribute<W>
{
    
    protected final int TYPE;
    
    protected WekaAbstractAttribute(String type)
    {
        TYPE = Arrays.binarySearch(TYPES, type);
        if(TYPE < 0)
        {
            throw new IllegalArgumentException("unknown attribute type: "+type);
        }
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.converter.WekaAttribute#getType()
     */
    public String getType()
    {
        return TYPES[TYPE];
    }

    public boolean isNominal()
    {
        return TYPE==NOMINAL_INDEX;
    }

    public boolean isNumeric()
    {
        return TYPE==NUMERIC_INDEX;
    }

    public boolean isString()
    {
        return TYPE==STRING_INDEX;
    }
    
    public String toString()
    {
        return getValue();
    }


    public boolean equals(Object o)
    {
        if(o instanceof WekaAttribute)
        {
            W a = (W) o;
            return this.compareTo(a)==0;
            
        }
        else
        {
            return false;
        }
    }
}
