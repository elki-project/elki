package de.lmu.ifi.dbs.distance;

import java.util.regex.Pattern;

/**
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class Distance implements Comparable, Cloneable
{
    /**
     * A Pattern to define the required format for a String defining a Distance.
     */
    protected Pattern format;
    
    /**
     * Any extending class should reimplement the  {@link java.lang.Object#clone() clone()}
     * method. 
     * 
     * @see java.lang.Object#clone()
     */
    public abstract Object clone(); 
    
    /**
     * Equality for Distances is given only for identical Objects.
     * 
     * This ensures the possibility to have several equidistant Objects
     * mapped under their respective Distance as key.
     * 
     * @param o
     * @return boolean
     */
    public final boolean equals(Object o)
    {
        return this == o;
    }
    
    /**
     * Returns the maximum of the given Distances or the first, if none is greater than the other one.
     * 
     * @param d1 first Distance
     * @param d2 second Distance
     * @return Distance the maximum of the given Distances or the first, if none is greater than the other one
     */
    public static Distance max(Distance d1, Distance d2)
    {
        if(d1.compareTo(d2) > 0)
        {
            return d1;
        }
        else if(d2.compareTo(d1) > 0)
        {
            return d2;
        }
        else
        {
            return d1;
        }
    }
    
    
    /**
     * Any extending class should implement a proper formatString-method to obtain a String describing the values of the Distance.
     * 
     * @return String a String describing the values of the Distance
     */
    public abstract String formatString();
    
    /**
     * Any extending class should implement a proper toString-method for printing the result-values.
     * 
     * @return String a human-readable representation of the Distance
     */
    public abstract String toString(); 
    
    /**
     * Any extending class should provide a value for an infinite Distance
     * 
     * 
     * @return infinite distance
     */
    public abstract Distance infiniteDistance();
    
    /**
     * Constructs a Distance for the given parameter.
     * 
     * @param parameter a String to construct a Distance from
     * @return a Distance constructed from the given String
     */
    public abstract Distance parse(String parameter);
    
    /**
     * Helper method to check parameter.
     * 
     * 
     * @param parameter parameter to be parsed to define Distance
     * @throws IllegalArgumentException if defined format does not match parameter 
     */
    protected void checkParameter(String parameter) throws IllegalArgumentException
    {
        if(!format.matcher(parameter).matches())
        {
            throw new IllegalArgumentException("Illegal argument to initialize Distance: "+parameter+". Pattern: "+format.pattern());
        }
    }
    
    
    /**
     * Returns a String to describe the required format of an input String
     * 
     * @return a String to describe the required format of an input String
     */
    public String requiredInputString()
    {
        return format.pattern();
    }
    
    /**
     * Any extending class should provide a value for an undefined Distance
     * 
     * @return undefined distance
     */
    public abstract Distance undef();
    
    
}
