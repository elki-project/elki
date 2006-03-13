package de.lmu.ifi.dbs.converter;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class WekaStringAttribute extends WekaAbstractAttribute<WekaStringAttribute>
{

    private String value;
    
    public WekaStringAttribute(String value)
    {
        super(STRING);
        this.value = value;
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.converter.WekaAttribute#getValue()
     */
    public String getValue()
    {
        return value;
    }

    /**
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(WekaStringAttribute o)
    {
        return this.value.compareTo(o.value);
    }

}
