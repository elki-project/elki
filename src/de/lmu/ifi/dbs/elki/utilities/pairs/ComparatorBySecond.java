package de.lmu.ifi.dbs.elki.utilities.pairs;


import java.util.Comparator;

/**
 * Provides a derived comparator for an {@link SimplePair} with property of type <code>P</code>
 * 
 * @param <P> the type of the second component to compare
 * 
 * @author Arthur Zimek
 */
public class ComparatorBySecond<P> implements Comparator<SimplePair<?,P>>
{
    /**
     * A comparator for type P.
     */
    private Comparator<P> comparator;

    /**
     * Provides a comparator for an {@link SimplePair} based on the given Comparator for type <code>P</code>.
     * 
     * @param comparator a Comparator for type <code>P</code> to base the comparison of an {@link SimplePair} on
     */
    public ComparatorBySecond(Comparator<P> comparator)
    {
        this.comparator = comparator;
    }
    
    /**
     * To Objects of type {@link SimplePair} are compared based on the comparison
     * of their property using the current {@link #comparator}.
     *  
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(SimplePair<?,P> o1, SimplePair<?,P> o2)
    {
        return comparator.compare(o1.getSecond(), o2.getSecond());
    }

}
