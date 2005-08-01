package de.lmu.ifi.dbs.algorithm;

import java.util.Hashtable;
import java.util.Map;

import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

/**
 * TODO comment
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractAlgorithm implements Algorithm
{
    public static final String VERBOSE_F = "verbose";
    
    public static final String VERBOSE_D = "flag to allow verbose messages while performing the algorithm";
    
    public static final String TIME_F = "time";
    
    public static final String TIME_D = "flag to request output of performance time";
    
    protected Map<String,String> parameterToDescription = new Hashtable<String,String>();
    
    protected OptionHandler optionHandler;

    private boolean verbose;
    
    private boolean time;
    
    /**
     * Sets the flags for verbose and time in the parameter map.
     * Any extending class should call this constructor, then add further parameters.
     * Any non-abstract extending class should finally initialize optionHandler.
     *
     */
    protected AbstractAlgorithm()
    {
        parameterToDescription.put(VERBOSE_F,VERBOSE_D);
        parameterToDescription.put(TIME_F,TIME_D);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return optionHandler.usage("",false);
    }

    /**
     * Sets the values for verbose and time flags.
     * Any extending class should call this method first and return the
     * returned array without further changes, but after setting further required parameters. 
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = optionHandler.grabOptions(args);
        verbose = optionHandler.isSet(VERBOSE_F);
        time = optionHandler.isSet(TIME_F);
        return remainingParameters;
    }

    /**
     * Returns whether the time should be assessed.
     * 
     * @return whether the time should be assessed
     */
    public boolean isTime()
    {
        return time;
    }

    /**
     * Returns whether verbose messages should be printed while executing the algorithm.
     * 
     * @return whether verbose messages should be printed while executing the algorithm
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    
    
}
