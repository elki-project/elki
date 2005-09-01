package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;

/**
 * TODO unfinished concept
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FrequencyDependentItemsetDistance extends DoubleDistanceFunction<BitVector>
{
    private Map<BitSet,Integer> frequencies;
    
    private Database<BitVector> database;

    /**
     * 
     */
    public FrequencyDependentItemsetDistance()
    {
        super();
    }

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

    public void setDatabase(Database<BitVector> database, boolean verbose)
    {
        frequencies = new Hashtable<BitSet,Integer>();
        this.database = database;
        
    }

    public String description()
    {
        // TODO Auto-generated method stub
        return null;
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
