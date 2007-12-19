package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

/**
 * A ClassLabel to identify a certain class of objects that is to discern from
 * other classes by a classifier.
 * 
 * @author Arthur Zimek 
 */
public abstract class ClassLabel<L extends ClassLabel<L>> extends AbstractLoggable implements Comparable<L>
{

    /**
     * ClassLabels need an empty constructor
     * for dynamic access.
     * Subsequently, the init method must be called.
     * Sets as debug status
     * {@link LoggingConfiguration.DEBUG}.
     */
    protected ClassLabel()
    {
        super(LoggingConfiguration.DEBUG);
        // requires subsequent call of init
    }
    
    /**
     * Initialization of a ClassLabel for the given label.
     * 
     * 
     * @param label
     *            the label to create a ClassLabel
     */
    public abstract void init(String label);

    /**
     * Any ClassLabel should ensure a natural ordering that is consistent with
     * equals. Thus, if <code>this.compareTo(o)==0</code>, then
     * <code>this.equals(o)</code> should be <code>true</code>.
     * 
     * @param obj
     *            an object to test for equality w.r.t. this ClassLabel
     * @return true, if <code>this==obj || this.compareTo(o)==0</code>, false
     *         otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
    	if(obj instanceof ClassLabel){
    		return false;
    	}
        return this == obj || this.compareTo((L) obj) == 0;
    }

    /**
     * Any ClassLabel requires a method to represent the label as a String. If
     * <code>ClassLabel a.equals((ClassLabel) b)</code>, then also
     * <code>a.toString().equals(b.toString())</code> should hold.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Returns the hashCode of the String-representation of this ClassLabel.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }
}
