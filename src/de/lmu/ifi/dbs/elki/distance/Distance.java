package de.lmu.ifi.dbs.elki.distance;

import java.io.Externalizable;


/**
 * The interface Distance defines the requirements of any instance class.
 * 
 * See {@link de.lmu.ifi.dbs.elki.distance.DistanceUtil} for related utility functions such as <code>min</code>, <code>max</code>.
 *
 * @author Arthur Zimek
 * @see de.lmu.ifi.dbs.elki.distance.DistanceUtil
 * @param <D> the type of Distance used
 */
public interface Distance<D extends Distance<D>> extends Comparable<D>, Externalizable {

    /**
     * Returns a new distance as sum of this distance and the given distance.
     *
     * @param distance the distance to be added to this distance
     * @return a new distance as sum of this distance and the given distance
     */
    D plus(D distance);

    /**
     * Returns a new Distance by subtracting the given distance
     * from this distance.
     *
     * @param distance the distance to be subtracted from this distance
     * @return a new Distance by subtracting the given distance
     *         from this distance
     */
    D minus(D distance);

    /**
     * Returns a String as description of this Distance.
     *
     * @return a String as description of this Distance
     */
    String description();

    /**
     * Any implementing class should implement a proper toString-method for printing the result-values.
     *
     * @return String a human-readable representation of the Distance
     */
    String toString();

    /**
     * Returns the number of Bytes this distance uses if it is written to
     * an external file.
     *
     * @return the number of Bytes this distance uses if it is written to
     *         an external file
     */
    int externalizableSize();

}
