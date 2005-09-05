package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;

import java.util.BitSet;

/**
 * Provides a DistanceFunction to compute
 * a Distance between BitVectors based on the number of shared bits.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class SharingDependentItemsetDistanceFunction extends DoubleDistanceFunction<BitVector>
{
    
    /**
     * Returns 1 if card is 0,
     * i divided by card otherwise.
     * 
     * 
     * @param i the number of bits
     * @param card the cardinality of a bitset
     * @return 1 if card is 0,
     * i divided by card otherwise
     */
    protected double ratio(int i, int card)
    {
        return card == 0 ? 1 : ((double) i) / card;
    }

    /**
     * Set the database that holds the associations for the DoubleVectors for
     * which the distances should be computed. This method does nothing because
     * in this distance function no associations are needed.
     *
     * @param database the database to be set
     * @param verbose  flag to allow verbose messages while performing the method
     */
    public void setDatabase(Database<BitVector> database, boolean verbose)
    {
    }

    /**
     * The method returns the given parameter-array unchanged since the class
     * does not require any parameters.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        return args;
    }

}
