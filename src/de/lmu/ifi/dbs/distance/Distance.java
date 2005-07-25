package de.lmu.ifi.dbs.distance;


/**
 * The interface Distance defines the requirements of any istance class.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Distance extends Comparable
{

    /**
     * Returns a new distance as sum of this distance and the given distance.
     * 
     * @param distance the distancce to be added to this distance
     * @return a new distance as sum of this distance and the given distance
     */
    public Distance plus(Distance distance);
    
    /**
     * Returns a new Distance by subtracting the given distance
     * from this distance.
     * 
     * 
     * @param distance the distance to be subtracted from this distance
     * @return a new Distance by subtracting the given distance
     * from this distance
     */
    public Distance minus(Distance distance);
    
    /**
     * Returns true, if this distance is an infinite distance,
     * false otherwise.
     * 
     * @return true, if this distance is an infinite distance,
     * false otherwise
     */
    public boolean isInfinite();
    
    /**
     * Any implementing class should implement a proper toString-method for printing the result-values.
     * 
     * @return String a human-readable representation of the Distance
     */
    public String toString(); 
            
}
