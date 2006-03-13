package de.lmu.ifi.dbs.converter;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class WekaNumericAttribute extends WekaAbstractAttribute<WekaNumericAttribute>
{
    private Double value;
    
    public WekaNumericAttribute(double value)
    {
        super(NUMERIC);
        this.value = value;
    }
    

    /**
     * 
     * @see de.lmu.ifi.dbs.converter.WekaAttribute#getValue()
     */
    public String getValue()
    {
        return Double.toString(value);
    }

    /**
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(WekaNumericAttribute o)
    {
        return this.value.compareTo(o.value);
    }

}
