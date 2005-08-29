package de.lmu.ifi.dbs.data;


/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class RealVector<T extends Number> implements FeatureVector<T>
{
    /**
     * The String to separate attribute values in a String that represents the
     * values.
     */
    public final static String ATTRIBUTE_SEPARATOR = " ";


    /**
     * The unique id of this object.
     */
    private Integer id;
    
    /**
     * Returns the unique id of this RealVector object.
     * 
     * @return the unique id of this RealVector object
     */
    public int getID()
    {
        return id;
    }

    /**
     * Sets the id of this RealVector object. The id must be unique within one
     * database.
     * 
     * @param id
     *            the id to be set
     */
    public void setID(int id)
    {
        this.id = id;
    }

    
    
}
