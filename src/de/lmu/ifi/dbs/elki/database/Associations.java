package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * A helper class to facilitate setting of global associations in a database.
 *
 * @author Arthur Zimek
 */
public class Associations extends AbstractLoggable {
  /**
   * Holds the objects associated under given association ids.
   */
    private Map<AssociationID<?>, Object> associations;

    /**
     * Provides an Associations object ready to set and get
     * Objects for association.
     *
     */
    public Associations() {
        super(LoggingConfiguration.DEBUG);
        associations = new Hashtable<AssociationID<?>, Object>();
    }

    /**
     * Sets the specified object under the given AssociationID.
     * 
     * 
     * @param <T> the type of the object to be associated and the AssociationID
     * @param associationID the AssociationID to point to the object
     * @param associationObject the object to be associated
     */
    public <T> void put(AssociationID<T> associationID, T associationObject) {
        this.associations.put(associationID, associationObject);
    }

    /**
     * Retrieves the object associated under the given association id.
     * 
     * 
     * @param <T> the type of the object to retrieve conforms to the type of the association id
     * @param associationID the association id pointing to the object to retrieve
     * @return the object associated under the given association id
     */
    @SuppressWarnings("unchecked")
    public <T> T get(AssociationID<T> associationID) {
        return (T) this.associations.get(associationID);
    }

    /**
     * Provides the set of all association ids pointing to objects within this Associations.
     * 
     * 
     * @return the set of all association ids pointing to objects within this Associations
     */
    public Set<AssociationID<?>> keySet() {
        return this.associations.keySet();
    }

    /**
     * Adds all pairs of association ids and objects currently
     * set in the given Associations object to this Associations object. 
     * 
     * @param associations an Associations object to copy the contained associations from
     */
    @SuppressWarnings("unchecked")
    public void putAll(Associations associations) {
        for (AssociationID associationID : associations.keySet()) {
            put(associationID, associations.get(associationID));
        }
    }
}
