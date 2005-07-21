package de.lmu.ifi.dbs.distance;


/**
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Distance extends Comparable
{
    
    /**
     * Any implementing class should implement a proper formatString-method to obtain a String describing the values of the Distance.
     * 
     * @return String a String describing the values of the Distance
     */
    public String formatString();
    
    /**
     * Any implementing class should implement a proper toString-method for printing the result-values.
     * 
     * @return String a human-readable representation of the Distance
     */
    public String toString(); 
    
    /**
     * Any implementing class should provide a value
     * for an infinite Distance
     * 
     * 
     * @return infinite distance
     */
    // TODO: public Distance infiniteDistance();
    
    /**
     * Constructs a Distance for the given parameter.
     * 
     * @param parameter a String to construct a Distance from
     * @return a Distance constructed from the given String
     */
    public Distance parse(String parameter);
        
    
    /**
     * Any implementing Class should return a String
     * to describe the required format of an input String
     * 
     * @return a String to describe the required format of an input String
     */
    public String requiredInputString();
    
    /**
     * Any implementing class should provide a value for an undefined Distance
     * 
     * @return undefined distance
     */
    // TODO: public Distance undef();
    
    
}
