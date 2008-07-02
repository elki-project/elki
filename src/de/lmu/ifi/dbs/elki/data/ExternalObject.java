package de.lmu.ifi.dbs.elki.data;

/**
 * Provides an id referencing an external data object.
 * 
 * @author Elke Achtert 
 */
public class ExternalObject extends AbstractDatabaseObject
{
    /**
     * Creates a new ExternalObject with the specified id.
     * 
     * @param externalID
     *            the external id
     */
    public ExternalObject(Integer externalID)
    {
        setID(externalID);
    }
}
