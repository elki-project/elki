package de.lmu.ifi.dbs.distance;

/**
 * An abstract distance implements equals conveniently for any extending class.
 * At the same time any extending class is to implement hashCode properly.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
abstract class AbstractDistance implements Distance
{
    
    /**
     * Any extending class should implement a proper hashCode method.
     * 
     * @see java.lang.Object#hashCode()
     */
    public abstract int hashCode();

    /**
     * Returns true if o is of the same class as this instance
     * and <code>this.compareTo(o)</code> is 0,
     * false otherwise. 
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o)
    {
        try
        {
            return this.compareTo((Distance) o) == 0;
        }
        catch(ClassCastException e)
        {
            return false;
        }
    }
}
