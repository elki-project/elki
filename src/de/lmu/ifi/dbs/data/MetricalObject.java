package de.lmu.ifi.dbs.data;

/**
 * To be a metrical object is the least requirement for an object to apply
 * distance based approaches. <p/>
 * Any implementing class should ensure to have a proper distance function
 * provided, that can handle the respective class.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface MetricalObject<T extends MetricalObject>
{
    /**
     * Equality of MetricalObjects should be defined by their values
     * regardless of their id.
     * 
     * @param obj another MetricalObject
     * @return true if all values of both MetricalObjects are equal, false otherwise
     */
    abstract boolean equals(Object obj);
    
    /**
     * Returns the unique id of this metrical object.
     * 
     * @return the unique id of this metrical object
     */
    Integer getID();

    /**
     * Sets the id of this metrical object. The id must be unique within one
     * database.
     * 
     * @param id
     *            the id to be set
     */
    void setID(Integer id);

    /**
     * Provides a deep copy of this object.
     * 
     * @return a copy of this object
     */
    T copy();

}
