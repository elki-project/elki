package de.lmu.ifi.dbs.elki.utilities;

/**
 * Holds a pair of an id (int) and another type.
 * 
 * @param <P> the type of the second component
 * @author Arthur Zimek
 */
public class IDPropertyPair<P>
{
    /**
     * First component of the pair: an id.
     */
    private int id;
    
    /**
     * Second component of the pair: a property of arbitrary type
     */
    private P property;
    
    /**
     * Constructs a pair of an int and another type
     * 
     * @param id the integer part (id)
     * @param property the second component
     */
    public IDPropertyPair(int id, P property)
    {
        this.id = id;
        this.property = property;
    }

    /**
     * Provides the id component of the pair.
     * 
     * @return the id component of the pair
     */
    public int getId()
    {
        return this.id;
    }

    /**
     * Provides the property component of the pair.
     * 
     * @return the property component of the pair
     */
    public P getProperty()
    {
        return this.property;
    }
}
