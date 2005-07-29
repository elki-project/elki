package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.distance.Distance;

/**
 * QueryResult holds the id of a database object and its distance to a special
 * query object.
 * 
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class QueryResult implements Comparable<QueryResult>
{
    /**
     * The id of the underlying database object.
     */
    private final int id;

    /**
     * The distance of the underlying database object to the query object.
     */
    private final Distance distance;

    /**
     * Creates a new QueryResult object.
     * 
     * @param id
     *            the id of the underlying database object
     * @param distance
     *            the distance of the underlying database object to the query
     *            object
     */
    public QueryResult(int id, Distance distance)
    {
        this.id = id;
        this.distance = distance;
    }

    /**
     * Returns the id of the underlying database object.
     * 
     * @return the id of the underlying database object
     */
    public int getId()
    {
        return id;
    }

    /**
     * Returns the distance of the underlying database object to the query
     * object.
     * 
     * @return the distance of the underlying database object to the query
     *         object
     */
    public Distance getDistance()
    {
        return distance;
    }

    /**
     * Compares this QueryResult with the given QueryResult with respect to
     * the distances.
     * 
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(QueryResult o)
    {
        return distance.compareTo(o.getDistance());
    }
    
    
}
