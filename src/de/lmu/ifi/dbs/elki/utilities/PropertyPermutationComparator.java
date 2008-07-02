package de.lmu.ifi.dbs.elki.utilities;


import java.util.Comparator;

/**
 * Provides a comparator for an {@link IDPropertyPair} with property of type <code>P</code>
 * 
 * @param <P> the type of the IDPropertyPair to compare
 * 
 * @author Arthur Zimek
 */
public class PropertyPermutationComparator<P> implements Comparator<IDPropertyPair<P>>
{
    /**
     * A comparator for type P.
     */
    private Comparator<P> propertyComparator;

    /**
     * Provides a comparator for an {@link IDPropertyPair} based on the given Comparator for type <code>P</code>.
     * 
     * @param propertyComparator a Comparator for type <code>P</code> to base the comparison of an {@link IDPropertyPair} on
     */
    public PropertyPermutationComparator(Comparator<P> propertyComparator)
    {
        this.propertyComparator = propertyComparator;
    }
    
    /**
     * To Objects of type {@link IDPropertyPair} are compared based on the comparison
     * of their property using the current {@link #propertyComparator}.
     *  
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(IDPropertyPair<P> o1, IDPropertyPair<P> o2)
    {
        return propertyComparator.compare(o1.getProperty(), o2.getProperty());
    }

}
