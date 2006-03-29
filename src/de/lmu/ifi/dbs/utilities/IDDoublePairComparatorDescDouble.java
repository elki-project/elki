package de.lmu.ifi.dbs.utilities;

import java.util.Comparator;

/**
 * A comparator for IDDoublePairs. The comparator ensures a sorting in
 * descending double values of the according IDDoublePairs when using
 * Arrays.sort
 * 
 * @author Peer Kr&ouml;ger (<a
 *         href="mailto:kroegerp@dbs.ifi.lmu.de">kroegerp@dbs.ifi.lmu.de</a>)
 */

public class IDDoublePairComparatorDescDouble implements
        Comparator<IDDoublePair>
{

    /**
     * Implements the compare method such that a sorting is in descending order.
     * 
     * @param o1
     *            the first object of type IDDoublePair
     * @param o2
     *            the second object of type IDDoublePair
     * @return 1, -1, and zero if the double value of the first IDDoublePair is
     *         greater, less or equal to the double value of the second one.
     */
    public int compare(IDDoublePair o1, IDDoublePair o2)
    {
        if (o1.getValue() < o2.getValue())
            return 1;
        else if (o1.getValue() > o2.getValue())
            return -1;
        else
            return 0;
    }
}
