package de.lmu.ifi.dbs.elki.converter;

/**
 * A WekaObject is an object that is able to provide its attributes as an array
 * of type WekaAttribute.
 * 
 * @author Arthur Zimek 
 */
public interface WekaObject<W extends WekaAttribute<W>>
{
    /**
     * Provides the attributes of this object as array of type WekaAttribute.
     * 
     * 
     * @return the attributes of this object
     */
    public W[] getAttributes();
}
