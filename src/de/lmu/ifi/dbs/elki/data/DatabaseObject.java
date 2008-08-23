package de.lmu.ifi.dbs.elki.data;

/**
 * A DatabaseObject should provide handling of a database ID. All implementing
 * classes should overwrite {@link Object#equals(Object) equals(Object)} to
 * ensure equality evaluation based on the specific values of the DatabaseObject
 * rather than their ID or identity.
 *
 * @author Arthur Zimek
 */
public interface DatabaseObject {
    /**
     * Equality of DatabaseObject should be defined by their values regardless
     * of their id.
     *
     * @param obj another DatabaseObject
     * @return true if all values of both DatabaseObjects are equal, false
     *         otherwise
     */
    abstract boolean equals(Object obj);

    /**
     * Returns the unique id of this database object.
     *
     * @return the unique id of this database object
     */
    Integer getID();

    /**
     * Sets the id of this database object. The id must be unique within one
     * database.
     *
     * @param id the id to be set
     */
    void setID(Integer id);
}
