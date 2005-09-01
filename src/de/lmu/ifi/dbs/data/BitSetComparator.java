package de.lmu.ifi.dbs.data;

import java.util.BitSet;
import java.util.Comparator;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class BitSetComparator implements Comparator<BitSet>
{

    /**
     * 
     * @see java.util.Comparator#compare(T, T)
     */
    public int compare(BitSet b1, BitSet b2)
    {
        if(b1.size() < b2.size())
        {
            return -1;
        }
        else if(b1.size() > b2.size())
        {
            return 1;
        }
        else
        {            
            int i1 = 0;
            int i2 = 0;
            while(i1 >= 0 && i2 >= 0)
            {
                i1 = b1.nextSetBit(i1);
                i2 = b2.nextSetBit(i2);
                if(i1 < i2)
                {
                    return -1;
                }
                else if(i1 > i2)
                {
                    return 1;
                }
            }
        }
        return 0;
    }

}
