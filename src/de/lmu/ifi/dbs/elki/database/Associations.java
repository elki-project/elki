package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * todo arthur comment
 *
 * @author Arthur Zimek
 */
public class Associations extends AbstractLoggable {
    @SuppressWarnings("unchecked")
    private Map<AssociationID, Object> associations;

    @SuppressWarnings("unchecked")
    public Associations() {
        super(LoggingConfiguration.DEBUG);
        associations = new Hashtable<AssociationID, Object>();
    }

    public <T> void put(AssociationID<T> associationID, T associationObject) {
        this.associations.put(associationID, associationObject);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(AssociationID<T> associationID) {
        return (T) this.associations.get(associationID);
    }

    @SuppressWarnings("unchecked")
    public Set<AssociationID> keySet() {
        return this.associations.keySet();
    }

    public <T> void putAll(Associations associations) {
        for (AssociationID<T> associationID : associations.keySet()) {
            put(associationID, associations.get(associationID));
        }
    }
}
