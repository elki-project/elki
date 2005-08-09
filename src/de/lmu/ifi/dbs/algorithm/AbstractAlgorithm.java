package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.Hashtable;
import java.util.Map;

/**
 * AbstractAlgorithm sets the values for flags verbose and time.
 * 
 * Any Algorithm that makes use of these flags may extend this class.
 * Beware to make correct use of parameter settings via optionHandler
 * as commented with constructor and methods.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractAlgorithm implements Algorithm
{
    /**
     * Flag to allow verbose messages.
     */
    public static final String VERBOSE_F = "verbose";
    
    /**
     * Description for verbose flag.
     */
    public static final String VERBOSE_D = "flag to allow verbose messages while performing the algorithm";
    
    /**
     * Flag to assess runtime.
     */
    public static final String TIME_F = "time";
    
    /**
     * Description for time flag.
     */
    public static final String TIME_D = "flag to request output of performance time";
    
    /**
     * Map providing a mapping of parameters to their descriptions.
     */
    protected Map<String,String> parameterToDescription = new Hashtable<String,String>();
    
    /**
     * OptionHandler to handler options. optionHandler should be initialized using
     * parameterToDescription in any non-abstract class extending this class.
     */
    protected OptionHandler optionHandler;

    /**
     * Property whether verbose messages should be allowed.
     */
    private boolean verbose;
    
    /**
     * Property whether runtime should be assessed.
     */
    private boolean time;
    
    
    /**
     * Sets the flags for verbose and time in the parameter map.
     * Any extending class should call this constructor, then add further parameters.
     * Any non-abstract extending class should finally initialize optionHandler
     * like this:
     * <pre>
     * {
     *     parameterToDescription.put(YOUR_PARAMETER_NAME+OptionHandler.EXPECTS_VALUE,YOUR_PARAMETER_DESCRIPTION);
     *     ...
     *     optionHandler = new OptionHandler(parameterToDescription,yourClass.class.getName());
     * }
     * </pre>
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
     *  An example for overwritting this method taking advantage from the previously
     *  (in superclasses) defined options would be:
     *  <pre>
     *  {
     *      String[] remainingParameters = super.setParameters(args);
     *      // set parameters for your class eventually using optionHandler
     *      
     *      ...
     *      
     *      return remainingParameters;
     *      // or in case of attributes requestingparameters themselves
     *      // return parameterizableAttribbute.setParameters(remainingParameters);
     *  }
     *  </pre>
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
