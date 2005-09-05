package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides a DistanceFunction to compute
 * a Distance between BitVectors based on the number of shared bits.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FrequencyDependentItemsetDistanceFunction extends SharingDependentItemsetDistanceFunction
{
    /**
     * Keeps the frequencies of itemset that have already been encountered.
     */
    private Map<BitSet,Integer> frequencies;
    
    /**
     * The database to compute itemset frequencies.
     */
    private Database<BitVector> database;
    

    /**
     * Provides a DistanceFunction to compute
     * a Distance between BitVectors based on the number of shared bits.
     */
    public FrequencyDependentItemsetDistanceFunction()
    {
        super();
    }

    /**
     * Returns a distance between two Bitvectors.
     * Distance is 1.0/support(%) * max{1-ratio(i,card1),1-ratio(i,card2)},
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
        double support = support(b1);
        return new DoubleDistance(1.0 / support * Math.max(1 - ratio(i,card1), 1 - ratio(i,card2)));
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.distance.SharingDependentItemsetDistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database, boolean)
     */
    public void setDatabase(Database<BitVector> database, boolean verbose)
    {
        frequencies = new Hashtable<BitSet,Integer>();
        this.database = database;        
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "Distance is 1.0/support(%) * max{1-ratio(i,o1),1-ratio(i,o2)}, where i is the number of bits shared by both BitVectors, o is the number of bits in the respective BitVector, and ratio(i,o) is 1 if o is 0, i/o otherwise.";
    }

    /**
     * Provides the support (percentage) of the given itemset
     * by the currently set database.
     * 
     * 
     * @param itemset the itemset to compute the support
     * @return the support (percentage) of the given itemset
     * by the currently set database
     */
    protected double support(BitSet itemset)
    {
        Integer freq = frequencies.get(itemset);
        double dbSize = database.size();
        if(freq != null)
        {
            return freq/dbSize;
        }
        else
        {
            freq = 0;
            for(Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();)
            {
                BitVector bv = database.get(dbIter.next());
                if(bv.contains(itemset))
                {
                    freq++;
                }
            }
            frequencies.put(itemset,freq);
            return freq/dbSize;
        }
    }
    
}
