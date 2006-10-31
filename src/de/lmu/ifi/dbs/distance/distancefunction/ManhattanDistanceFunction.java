package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;

/**
 * Manhattan distance function to compute the Manhattan distance
 * for a pair of NumberVectors.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ManhattanDistanceFunction<T extends NumberVector> extends DoubleDistanceFunction<T>
{

    /**
     * Provides a Manhattan distance function that can compute the Manhattan
     * distance (that is a DoubleDistance) for FeatureVectors.
     */
    public ManhattanDistanceFunction()
    {
        super();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
     */
    public DoubleDistance distance(T o1, T o2)
    {
        if(o1.getDimensionality() != o2.getDimensionality())
        {
            throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
        }
        double sum = 0;
        for(int i = 1; i <= o1.getDimensionality(); i++)
        {
            sum += Math.abs(o1.getValue(i).doubleValue() - o2.getValue(i).doubleValue());
        }
        return new DoubleDistance(sum);

    }

}
