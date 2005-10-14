package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Interface DistanceFunction describes the requirements of any distance
 * function.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface DistanceFunction<T extends MetricalObject> extends Parameterizable
{
    /**
     * The default package for distance functions.
     */
    public static final String DEFAULT_PACKAGE = DistanceFunction.class.getPackage().getName();

    /**
     * Provides a distance suitable to this DistanceFunction based on the given
     * pattern.
     * 
     * @param pattern
     *            A pattern defining a distance suitable to this
     *            DistanceFunction
     * @return a distance suitable to this DistanceFunction based on the given
     *         pattern
     * @throws IllegalArgumentException
     *             if the given pattern is not compatible with the requirements
     *             of this DistanceFunction
     */
    Distance valueOf(String pattern) throws IllegalArgumentException;

    /**
     * Provides an infinite distance.
     * 
     * @return an infinite distance
     */
    Distance infiniteDistance();

    /**
     * Provides a null distance.
     * 
     * @return a null distance
     */
    Distance nullDistance();

    /**
     * Provides an undefined distance.
     * 
     * @return an undefined distance
     */
    Distance undefinedDistance();

    /**
     * Returns true, if the given distance is an infinite distance, false
     * otherwise.
     * 
     * @param distance
     *            the distance to be tested on infinity
     * @return true, if the given distance is an infinite distance, false
     *         otherwise
     */
    boolean isInfiniteDistance(Distance distance);

    /**
     * Returns true, if the given distance is a null distance, false otherwise.
     * 
     * @param distance
     *            the distance to be tested whether it is a null distance
     * @return true, if the given distance is a null distance, false otherwise
     */
    boolean isNullDistance(Distance distance);

    /**
     * Returns true, if the given distance is an undefined distance, false
     * otherwise.
     * 
     * @param distance
     *            the distance to be tested whether it is undefined
     * @return true, if the given distance is an undefined distance, false
     *         otherwise
     */
    boolean isUndefinedDistance(Distance distance);

    /**
     * Returns a String as description of the required input format.
     * 
     * @return a String as description of the required input format
     */
    String requiredInputPattern();

    /**
     * Computes the distance between two given MetricalObjects according to this
     * distance function.
     * 
     * @param o1
     *            first MetricalObject
     * @param o2
     *            second MetricalObject
     * @return the distance between two given MetricalObjects according to this
     *         distance function
     */
    Distance distance(T o1, T o2);

    /**
     * Set the database that holds the associations for the MetricalObject for
     * which the distances should be computed.
     * 
     * @param database
     *            the database to be set
     * @param verbose
     *            flag to allow verbose messages while performing the method
     */
    void setDatabase(Database<T> database, boolean verbose);
}