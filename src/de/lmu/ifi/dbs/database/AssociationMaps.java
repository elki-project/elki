package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * @author Arthur Zimek
 */
public class AssociationMaps extends AbstractLoggable
{

    private Map<AssociationID,Map<Integer,Object>> associations;
    
    public AssociationMaps()
    {
        super(LoggingConfiguration.DEBUG);
        associations = new Hashtable<AssociationID,Map<Integer,Object>>();
    }
    
    public <T> void put(AssociationID<T> associationID, Map<Integer,T> associationMap)
    {
        this.associations.put((AssociationID) associationID, (Map<Integer,Object>) associationMap);
    }
    
    public <T> Map<Integer,T> get(AssociationID<T> associationID)
    {
        return (Map<Integer,T>) this.associations.get(associationID);
    }
    
    public Set<AssociationID> keySet()
    {
        return this.associations.keySet();
    }
    
    public boolean containsKey(AssociationID<?> associationID)
    {
        return this.associations.containsKey(associationID);
    }

}
