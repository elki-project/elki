package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.data.ClassLabel;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class WekaNominalAttribute<L extends ClassLabel<L>> extends WekaAbstractAttribute<WekaNominalAttribute<L>>
{
    private L value;
    
    public WekaNominalAttribute(L value)
    {
        super(NOMINAL);
        this.value = value;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.converter.WekaAttribute#getValue()
     */
    public String getValue()
    {
        return value.toString();
    }

    /**
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(WekaNominalAttribute<L> o)
    {
        return this.value.compareTo(o.value);
    }

}
