package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * @author Arthur Zimek
 */
public class AssociationMaps extends AbstractLoggable
{

    @SuppressWarnings("unchecked")
    private Map<AssociationID,Map<Integer,Object>> associations;
    
    @SuppressWarnings("unchecked")
    public AssociationMaps()
    {
        super(LoggingConfiguration.DEBUG);
        associations = new Hashtable<AssociationID,Map<Integer,Object>>();
    }
    
    @SuppressWarnings("unchecked")
    public <T> void put(AssociationID<T> associationID, Map<Integer,T> associationMap)
    {
        this.associations.put((AssociationID) associationID, (Map<Integer,Object>) associationMap);
    }
    
    @SuppressWarnings("unchecked")
    public <T> Map<Integer,T> get(AssociationID<T> associationID)
    {
        return (Map<Integer,T>) this.associations.get(associationID);
    }
    
    @SuppressWarnings("unchecked")
    public Set<AssociationID> keySet()
    {
        return this.associations.keySet();
    }
    
    public boolean containsKey(AssociationID<?> associationID)
    {
        return this.associations.containsKey(associationID);
    }

}
