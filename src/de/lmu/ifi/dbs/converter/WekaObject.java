package de.lmu.ifi.dbs.converter;

/**
 * A WekaObject is an object that is able to provide its attributes as an array
 * of type WekaAttribute.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface WekaObject
{
    /**
     * Provides the attributes of this object as array of type WekaAttribute.
     * 
     * 
     * @return the attributes of this object
     */
    public WekaAttribute[] getAttributes();
}
