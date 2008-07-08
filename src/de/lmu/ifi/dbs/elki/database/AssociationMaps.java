package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to facilitate an association mapping from AssociationID
 * to a map from an object id to an associated object.
 *  
 * @author Arthur Zimek
 */
public class AssociationMaps extends AbstractLoggable {

  /**
   * Holds a mapping from AssociationID to maps for object ids and associated objects.
   */
    @SuppressWarnings("unchecked")
    private Map<AssociationID, Map<Integer, Object>> associations;

    /**
     * Provides an AssociationMaps object ready to
     * set and get mappings from object ids to associated objects.
     *
     */
    @SuppressWarnings("unchecked")
    public AssociationMaps() {
        super(LoggingConfiguration.DEBUG);
        associations = new Hashtable<AssociationID, Map<Integer, Object>>();
    }

    /**
     * Associates a new mapping from object ids to associated objects
     * for a given AssociationID.
     * 
     * 
     * @param <T> the type of the AssociationID conforming the type of associated objects
     * @param associationID the AssociationID for the mapping of object id to objects
     * @param associationMap the mapping of object ids to objects
     */
    @SuppressWarnings("unchecked")
    public <T> void put(AssociationID<T> associationID, Map<Integer, T> associationMap) {
        this.associations.put(associationID, (Map<Integer, Object>) associationMap);
    }

    /**
     * Provides the mapping from object ids to associated objects
     * for the given AssociationID.
     * 
     * @param <T> the type of the AssociationID conforming the type of associated objects
     * @param associationID the AssociationID for the mapping of object id to objects
     * @return the mapping of object ids to objects for the specified AssociationID
     */
    @SuppressWarnings("unchecked")
    public <T> Map<Integer, T> get(AssociationID<T> associationID) {
        return (Map<Integer, T>) this.associations.get(associationID);
    }

    /**
     * Provides the set of all association ids pointing to mappings from object ids to objects within this AssociationMaps.
     * 
     * 
     * @return the set of all association ids pointing to mappings from object ids to objects within this AssociationMaps
     */
    @SuppressWarnings("unchecked")
    public Set<AssociationID> keySet() {
        return this.associations.keySet();
    }

    /**
     * Checks whether this AssociationMaps contains a mapping for a given AssociationID.
     * 
     * 
     * @param associationID the AssociationID to check whether or not it is used as a key already
     * @return {@code true} if this AssociationMaps contains a mapping for the given AssociationID, {@code false} otherwise
     */
    public boolean containsKey(AssociationID<?> associationID) {
        return this.associations.containsKey(associationID);
    }

}
