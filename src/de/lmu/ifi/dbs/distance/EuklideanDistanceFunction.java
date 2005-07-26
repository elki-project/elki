package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;

/**
 * Provides the Euklidean distance for real valued vectors.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class EuklideanDistanceFunction extends DoubleDistanceFunction
{
    /**
     * Provides a Euklidean distance function
     * that can compute the Euklidean distance
     * (that is a DoubleDistance)
     * for RealVectors.
     *
     */
    public EuklideanDistanceFunction()
    {
        super();
    }

    /**
     * Provides the Euklidean distance between the given two vectors.
     * 
     * @return the Euklidean distance between the given two vectors
     * as an instance of {@link DoubleDistance DoubleDistance}.
     * 
     * @see de.lmu.ifi.dbs.distance.RealVectorDistanceFunction#distance(de.lmu.ifi.dbs.data.RealVector, de.lmu.ifi.dbs.data.RealVector)
     */
    public Distance distance(RealVector rv1, RealVector rv2)
    {
        if(rv1.getDimensionality() != rv2.getDimensionality())
        {
            throw new IllegalArgumentException("Different dimensionality of RealVectors\n  first argument: "+rv1.toString()+"\n  second argument: "+rv2.toString());
        }
        double sqrDist = 0;
        for(int i = 1; i <= rv1.getDimensionality(); i++)
        {
            double manhattanI = rv1.getValue(i) - rv2.getValue(i);
            sqrDist += manhattanI * manhattanI;
        }
        return new DoubleDistance(Math.sqrt(sqrDist));
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "Euklidean distance for RealVectors. No parameters required.";
    }

    /**
     * The method returns the given parameter-array unchanged since
     * the class does not require any parameters.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        return args;
    }

}
