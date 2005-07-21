package de.lmu.ifi.dbs.distance;

import java.util.regex.Pattern;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDistance implements Distance
{
    
    /**
     * A Pattern to define the required format for a String defining a Distance.
     */
    protected Pattern format;
    
    
    /**
     * Equality for Distances is given only for identical Objects.
     * 
     * TODO: necessary?
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
     * Returns a String to describe the required format of an input String
     * 
     * @return a String to describe the required format of an input String
     */
    public String requiredInputString()
    {
        return format.pattern();
    }
    
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
}
