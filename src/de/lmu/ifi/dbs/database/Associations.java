package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * @author Arthur Zimek
 */
public class Associations extends AbstractLoggable
{
    private Map<AssociationID, Object> associations;

    public Associations()
    {
        super(LoggingConfiguration.DEBUG);
        associations = new Hashtable<AssociationID, Object>();
    }

    public <T> void put(AssociationID<T> associationID, T associationObject)
    {
        this.associations.put(associationID, associationObject);
    }

    public <T> T get(AssociationID<T> associationID)
    {
        return (T) this.associations.get(associationID);
    }

    public Set<AssociationID> keySet()
    {
        return this.associations.keySet();
    }
    
    public <T> void putAll(Associations associations)
    {
        for(AssociationID<T> associationID : associations.keySet())
        {
            put(associationID,associations.get(associationID));
        }
    }
}
