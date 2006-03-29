package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.IndexDatabase;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * AbstractAlgorithm sets the values for flags verbose and time. <p/> Any
 * Algorithm that makes use of these flags may extend this class. Beware to make
 * correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractAlgorithm<O extends DatabaseObject> implements Algorithm<O>
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
    protected Map<String, String> parameterToDescription;

    /**
     * OptionHandler to handle options. optionHandler should be initialized
     * using parameterToDescription in any non-abstract class extending this
     * class.
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
     * Holds the currently set parameter array.
     */
    private String[] currentParameterArray = new String[0];

    /**
     * Sets the flags for verbose and time in the parameter map. Any extending
     * class should call this constructor, then add further parameters. If any
     * non-abstract extending class adds further parameters, it has to finally
     * initialize optionHandler like this: <p/> <p/> <p/> <p/>
     * 
     * <pre>
     *       {
     *           parameterToDescription.put(YOUR_PARAMETER_NAME+OptionHandler.EXPECTS_VALUE,YOUR_PARAMETER_DESCRIPTION);
     *           ...
     *           optionHandler = new OptionHandler(parameterToDescription,yourClass.class.getName());
     *       }
     * </pre>
     */
    protected AbstractAlgorithm()
    {
        parameterToDescription = new Hashtable<String, String>();
        parameterToDescription.put(VERBOSE_F, VERBOSE_D);
        parameterToDescription.put(TIME_F, TIME_D);
        optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return optionHandler.usage("", false);
    }

    /**
     * Sets the values for verbose and time flags. Any extending class should
     * call this method first and return the returned array without further
     * changes, but after setting further required parameters. <p/> An example
     * for overwritting this method taking advantage from the previously (in
     * superclasses) defined options would be: <p/> <p/> <p/> <p/>
     * 
     * <pre>
     *        {
     *            String[] remainingParameters = super.setParameters(args);
     *            // set parameters for your class eventually using optionHandler
     *      &lt;p/&gt;
     *            ...
     *      &lt;p/&gt;
     *            return remainingParameters;
     *            // or in case of attributes requestingparameters themselves
     *            // return parameterizableAttribbute.setParameters(remainingParameters);
     *        }
     * </pre>
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = optionHandler.grabOptions(args);
        verbose = optionHandler.isSet(VERBOSE_F);
        time = optionHandler.isSet(TIME_F);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Sets the difference of the first array minus the second array as the
     * currently set parameter array.
     * 
     * @param complete
     *            the complete array
     * @param part
     *            an array that contains only elements of the first array
     */
    protected void setParameters(String[] complete, String[] part)
    {
        currentParameterArray = Util.difference(complete, part);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
     */
    public String[] getParameters()
    {
        String[] param = new String[currentParameterArray.length];
        System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
        return param;
    }

    /**
     * Returns the parameter setting of the attributes.
     * 
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = new ArrayList<AttributeSettings>();

        AttributeSettings setting = new AttributeSettings(this);
        setting.addSetting(TIME_F, Boolean.toString(isTime()));
        setting.addSetting(VERBOSE_F, Boolean.toString(isVerbose()));
        settings.add(setting);

        return settings;
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
     * Returns whether verbose messages should be printed while executing the
     * algorithm.
     * 
     * @return whether verbose messages should be printed while executing the
     *         algorithm
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * Sets whether the time should be assessed.
     * 
     * @param time
     *            whether the time should be assessed
     */
    public void setTime(boolean time)
    {
        this.time = time;
    }

    /**
     * Sets whether verbose messages should be printed while executing the
     * algorithm.
     * 
     * @param verbose
     *            whether verbose messages should be printed while executing the
     *            algorithm
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * Calls the runInTime()-method of extending classes. Measures and prints
     * the runtime of this method.
     * 
     * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public final void run(Database<O> database) throws IllegalStateException
    {
        long start = System.currentTimeMillis();
        runInTime(database);
        long end = System.currentTimeMillis();
        if(isTime())
        {
            long elapsedTime = end - start;
            System.out.println(this.getClass().getName() + " runtime  : " + elapsedTime + " milliseconds.");
        }
        if(database instanceof IndexDatabase && isTime())
        {
            System.out.println(this.getClass().getName() + " I/O-time : " + ((IndexDatabase) database).getIOAccess());
        }
    }

    /**
     * The run method encapsulated in measure of runtime. An extending class
     * needs not to take care of runtime itself.
     * 
     * @param database
     *            the database to run the algorithm on
     * @throws IllegalStateException
     *             if the algorithm has not been initialized properly (e.g. the
     *             setParameters(String[]) method has been failed to be called).
     */
    protected abstract void runInTime(Database<O> database) throws IllegalStateException;

}
