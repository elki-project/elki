package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.database.connection.DatabaseConnection DatabaseConnection}.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KDDTask implements Parameterizable
{
    /**
     * Holds the class specific debug status.
     */
    private static final boolean DEBUG = LoggingConfiguration.DEBUG;

    /**
     * The logger of this class.
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * The String for calling this class' main routine on command line
     * interface.
     */
    private static final String CALL = "java " + KDDTask.class.getName();

    /**
     * The newline string according to system.
     */
    public static final String NEWLINE = System.getProperty("line.separator");

    /**
     * The parameter algorithm.
     */
    public static final String ALGORITHM_P = "algorithm";
        
    /**
     * Description for parameter algorithm.
     */
    public static final String ALGORITHM_D = "<class>classname of an algorithm " + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Algorithm.class) + " Either full name to identify classpath or only classname, if its package is " + Algorithm.class.getPackage().getName() + ".";
    
    /**
     * Help flag.
     */
    public static final String HELP_F = "h";

    /**
     * Long help flag.
     */
    public static final String HELPLONG_F = "help";

    /**
     * Description for help flag.
     */
    public static final String HELP_D = "flag to obtain help-message, either for the main-routine or for any specified algorithm. Causes immediate stop of the program.";

    /**
     * Description flag.
     */
    public static final String DESCRIPTION_P = "description";

    /**
     * Description for description parameter.
     */
    public static final String DESCRIPTION_D = "<class>name of a class to obtain a description - for classes that implement "+Parameterizable.class.getName()+" -- no further processing will be performed.";

    /**
     * The default database connection.
     */
    private static final String DEFAULT_DATABASE_CONNECTION = FileBasedDatabaseConnection.class.getName();

    /**
     * Parameter for database connection.
     */
    public static final String DATABASE_CONNECTION_P = "dbc";

    /**
     * Description for parameter database connection.
     */
    public static final String DATABASE_CONNECTION_D = "<class>classname of a class "+ Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DatabaseConnection.class) + ". Either full name to identify classpath or only classname, if its package is " + DatabaseConnection.class.getPackage().getName() + ". (Default: " + DEFAULT_DATABASE_CONNECTION + ").";

    /**
     * Parameter output.
     */
    public static final String OUTPUT_P = "out";

    /**
     * Description for parameter output.
     */
    public static final String OUTPUT_D = "<filename>file to write the obtained results in. If an algorithm requires several outputfiles, the given filename will be used as prefix followed by automatically created markers. If this parameter is omitted, per default the output will sequentially be given to STDOUT.";

    /**
     * Parameter normalization.
     */
    public static final String NORMALIZATION_P = "norm";

    /**
     * Description for parameter normalization.
     */
    public static final String NORMALIZATION_D = "<class>a normalization to use a database with normalized values " + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Normalization.class);

    /**
     * Flag normalization undo.
     */
    public static final String NORMALIZATION_UNDO_F = "normUndo";

    /**
     * Description for flag normalization undo.
     */
    public static final String NORMALIZATION_UNDO_D = "flag to revert result to original values - invalid option if no normalization has been performed.";

    /**
     * Holds the currently set parameter array.
     */
    private String[] currentParameterArray = new String[0];

    /**
     * The algorithm to run.
     */
    @SuppressWarnings("unchecked")
    private Algorithm algorithm;

    /**
     * The database connection to have the algorithm run with.
     */
    @SuppressWarnings("unchecked")
    private DatabaseConnection databaseConnection;

    /**
     * The file to print results to.
     */
    private File out;

    /**
     * Whether KDDTask has been properly initialized for calling the
     * {@link #run() run()}-method.
     */
    private boolean initialized = false;

    /**
     * A normalization - per default no normalization is used.
     */
    @SuppressWarnings("unchecked")
    private Normalization normalization = null;

    /**
     * Whether to undo normalization for result.
     */
    private boolean normalizationUndo = false;

    /**
     * OptionHandler for handling options.
     */
    private OptionHandler optionHandler;

    /**
     * Provides a KDDTask.
     */
    public KDDTask()
    {
        Map<String, String> parameterToDescription = new Hashtable<String, String>();
        parameterToDescription.put(ALGORITHM_P + OptionHandler.EXPECTS_VALUE,ALGORITHM_D);
        parameterToDescription.put(HELP_F, HELP_D);
        parameterToDescription.put(HELPLONG_F, HELP_D);
        parameterToDescription.put(DESCRIPTION_P + OptionHandler.EXPECTS_VALUE, DESCRIPTION_D);
        parameterToDescription.put(DATABASE_CONNECTION_P + OptionHandler.EXPECTS_VALUE, DATABASE_CONNECTION_D);
        parameterToDescription.put(OUTPUT_P + OptionHandler.EXPECTS_VALUE,OUTPUT_D);
        parameterToDescription.put(NORMALIZATION_P + OptionHandler.EXPECTS_VALUE, NORMALIZATION_D);
        parameterToDescription.put(NORMALIZATION_UNDO_F, NORMALIZATION_UNDO_D);
        optionHandler = new OptionHandler(parameterToDescription, CALL);
        if(DEBUG)
        {
            logger.finest("Root logger level: "+Logger.getLogger("").getLevel().getName()+"\n");
        }
    }

    /**
     * Returns a description for printing on command line interface.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return optionHandler.usage("");
    }

    /**
     * Returns a usage message with the specified message as leading line, and
     * information as provided by optionHandler. If an algorithm is specified,
     * the description of the algorithm is returned.
     * 
     * @param message
     *            a message to be include in the usage message
     * @return a usage message with the specified message as leading line, and
     *         information as provided by optionHandler
     */
    public String usage(String message)
    {
        StringBuffer usage = new StringBuffer();
        usage.append(message);
        usage.append(NEWLINE);
        usage.append(optionHandler.usage("", false));
        usage.append(NEWLINE);
        if (algorithm != null)
        {
            usage.append(OptionHandler.OPTION_PREFIX);
            usage.append(ALGORITHM_P);
            usage.append(" ");
            usage.append(algorithm.description());
            usage.append(NEWLINE);
        }
        return usage.toString();
    }

    /**
     * Sets the options accordingly to the specified list of parameters.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @SuppressWarnings("unchecked")
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = optionHandler.grabOptions(args);
        if (args.length == 0)
        {
            throw new AbortException("No options specified. Try flag -h to gain more information.");
        }
        
        // help
        if (optionHandler.isSet(HELP_F) || optionHandler.isSet(HELPLONG_F))
        {
            throw new AbortException(description());
        }

        // description
        if (optionHandler.isSet(DESCRIPTION_P))
        {
            String parameterizableName = optionHandler.getOptionValue(DESCRIPTION_P);
            Parameterizable p = null;
            try
            {
                try
                {
                    p = Util.instantiate(Algorithm.class, parameterizableName);
                }
                catch(UnableToComplyException e)
                {
                    p = Util.instantiate(Parameterizable.class, parameterizableName);    
                }
            }
            catch(UnableToComplyException e)
            {
                throw new WrongParameterValueException(DESCRIPTION_P, parameterizableName, DESCRIPTION_D, e);
            }
            if(p instanceof Algorithm)
            {
                Algorithm a = (Algorithm) p;
                throw new AbortException(a.getDescription().toString()+'\n'+a.description());
            }
            else
            {
                throw new AbortException(p.description());
            }
        }

        // algorithm
        String algorithmName = optionHandler.getOptionValue(ALGORITHM_P);
        try
        {
            algorithm = Util.instantiate(Algorithm.class, algorithmName);
        }
        catch (UnableToComplyException e)
        {
            throw new WrongParameterValueException(ALGORITHM_P, algorithmName, ALGORITHM_D, e);
        }

        // database connection
        String databaseConnectionName = optionHandler.isSet(DATABASE_CONNECTION_P) ? optionHandler.getOptionValue(DATABASE_CONNECTION_P) : DEFAULT_DATABASE_CONNECTION;
        try
        {
            databaseConnection = Util.instantiate(DatabaseConnection.class,databaseConnectionName);
        }
        catch (UnableToComplyException e)
        {
            throw new WrongParameterValueException(DATABASE_CONNECTION_P,databaseConnectionName, DATABASE_CONNECTION_D, e);
        }

        // output
        if (optionHandler.isSet(OUTPUT_P))
        {
            out = new File(optionHandler.getOptionValue(OUTPUT_P));
        }
        else
        {
            out = null;
        }

        // normalization
        if (optionHandler.isSet(NORMALIZATION_P))
        {
            String normalizationName = optionHandler.getOptionValue(NORMALIZATION_P);
            try
            {
                normalization = Util.instantiate(Normalization.class,normalizationName);
            }
            catch (UnableToComplyException e)
            {
                throw new WrongParameterValueException(NORMALIZATION_P,normalizationName, NORMALIZATION_D, e);
            }
            normalizationUndo = optionHandler.isSet(NORMALIZATION_UNDO_F);
            remainingParameters = normalization.setParameters(remainingParameters);
        }
        else if (optionHandler.isSet(NORMALIZATION_UNDO_F))
        {
            throw new WrongParameterValueException("Illegal parameter setting: Flag " + NORMALIZATION_UNDO_F + " is set, but no normalization is specified.");
        }

        remainingParameters = algorithm.setParameters(remainingParameters);
        remainingParameters = databaseConnection.setParameters(remainingParameters);

        initialized = true;
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Sets the difference of the first array minus the second array as the
     * currently set parameter array.
     * 
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
     * 
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
        List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

        AttributeSettings mySettings = new AttributeSettings(this);
        mySettings.addSetting(DATABASE_CONNECTION_P, databaseConnection.getClass().getName());
        if (normalization != null)
        {
            mySettings.addSetting(NORMALIZATION_P, normalization.getClass().getName());
            mySettings.addSetting(NORMALIZATION_UNDO_F, Boolean.toString(normalizationUndo));
        }
        if (out != null)
        {
            mySettings.addSetting(OUTPUT_P, out.toString());
        }
        mySettings.addSetting(ALGORITHM_P, algorithm.getClass().getName());
        attributeSettings.add(mySettings);

        attributeSettings.addAll(databaseConnection.getAttributeSettings());
        if (normalization != null)
        {
            attributeSettings.addAll(normalization.getAttributeSettings());
        }
        attributeSettings.addAll(algorithm.getAttributeSettings());

        return attributeSettings;
    }

    /**
     * Method to run the specified algorithm using the specified database
     * connection.
     * 
     * @throws IllegalStateException
     *             if initialization has not been done properly (i.e.
     *             {@link #setParameters(String[]) setParameters(String[])} has
     *             not been called before calling this method)
     */
    @SuppressWarnings("unchecked")
    public Result run() throws IllegalStateException
    {
        if (initialized)
        {
            algorithm.run(databaseConnection.getDatabase(normalization));
            try
            {
                Result result = algorithm.getResult();

                List<AttributeSettings> settings = getAttributeSettings();
                if (normalizationUndo)
                {
                    result.output(out, normalization, settings);
                }
                else
                {
                    result.output(out, null, settings);
                }
                return result;
            }
            catch (UnableToComplyException e)
            {
                throw new IllegalStateException("Error in restoring result to original values.", e);
            }
        }
        else
        {
            throw new IllegalStateException("KDD-Task was not properly initialized. Need to set parameters first.");
        }
    }

    public Logger getLogger()
    {
        return logger;
    }

    /**
     * Runs a KDD task accordingly to the specified parameters.
     * 
     * @param args
     *            parameter list according to description
     */
    public static void main(String[] args)
    {
        LoggingConfiguration.configureRootFinally(LoggingConfiguration.CLI);
        KDDTask kddTask = new KDDTask();
        try
        {
            kddTask.setParameters(args);
            kddTask.run();
        }
        catch (AbortException e)
        {
            kddTask.getLogger().info(e.getMessage());
//            kddTask.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        catch (ParameterException e)
        {
//            kddTask.getLogger().warning(e.getMessage());
            kddTask.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        catch(Exception e) // any other exception
        {
            kddTask.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

}
