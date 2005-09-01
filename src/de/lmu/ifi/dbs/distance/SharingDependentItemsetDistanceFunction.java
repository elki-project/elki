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
public class SharingDependentItemsetDistanceFunction extends DoubleDistanceFunction<BitVector>
{
    /**
     * Provides a DistanceFunction to compute
     * a Distance between BitVectors based on the number of shared bits.
     */
    public SharingDependentItemsetDistanceFunction()
    {
        super();
    }

    /**
     * Returns a distance between two Bitvectors.
     * Distance is max{1-ratio(i,card1),1-ratio(i,card2)},
     * where i is the number of bits shared by both BitVectors,
     * o is the number of bits in the respective BitVector,
     * and ratio(i,card) is 1 if card is 0, i/card otherwise.
     * 
     * @param o1 first BitVector
     * @param o2 second BitVector
     * @return Distance between o1 and o2
     */
    public Distance distance(BitVector o1, BitVector o2)
    {
        BitSet b1 = o1.getBits();
        BitSet b2 = o2.getBits();
        int card1 = b1.cardinality();
        int card2 = b2.cardinality();
        b1.and(b2);
        int i = b1.cardinality();
        return new DoubleDistance(Math.max(1 - ratio(i,card1), 1 - ratio(i,card2)));
    }
    
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
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "Distance is max{1-ratio(i,o1),1-ratio(i,o2)}, where i is the number of bits shared by both BitVectors, o is the number of bits in the respective BitVector, and ratio(i,o) is 1 if o is 0, i/o otherwise.";
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
