package de.lmu.ifi.dbs.data;

/**
 * A simple class label casting a String as it is as label.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class SimpleClassLabel extends ClassLabel<SimpleClassLabel>
{
    /**
     * Holds the String designating the label.
     */
    private String label;

//    /**
//     * Provides a simple class label covering the given String.
//     * @param label the String to be cast as label
//     */
//    public SimpleClassLabel(String label)
//    {
//        super();
//        this.init(label);
//    }
    
    /**
     * Provides a simple class label covering the given String.
     * @param label the String to be cast as label
     * 
     * @see de.lmu.ifi.dbs.data.ClassLabel#init(java.lang.String)
     */
    @Override
    public void init(String label)
    {
        this.label = label;
    }



    /**
     * The ordering of two SimpleClassLabels is given
     * by the ordering on the Strings they represent.
     * 
     * That is, the result equals
     * <code>this.label.compareTo(o.label)</code>.
     * 
     * @see java.lang.Comparable#compareTo(T)
     */
    public int compareTo(SimpleClassLabel o)
    {
        return this.label.compareTo(o.label);
    }

    /**
     * The hash code of a simple class label is the hash code
     * of the String represented by the ClassLabel.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return label.hashCode();
    }

    /**
     * Returns a new instance of the String covered by this SimpleClassLabel.
     * 
     * @return a new instance of the String covered by this SimpleClassLabel
     * 
     * @see ClassLabel#toString()
     */
    @Override
    public String toString()
    {
        return new String(label);
    }
    
    

}
