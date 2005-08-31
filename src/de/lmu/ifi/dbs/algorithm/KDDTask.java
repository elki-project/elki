package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.DatabaseConnection;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.database.DatabaseConnection DatabaseConnection}.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KDDTask implements Parameterizable
{
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
     * The default package for algorithms.
     */
    public static final String DEFAULT_ALGORITHM_PACKAGE = KDDTask.class.getPackage().getName();

    /**
     * The default package for database connections.
     */
    public static final String DEFAULT_DATABASE_CONNECTION_PACKAGE = DatabaseConnection.class.getPackage().getName();

    /**
     * The parameter algorithm.
     */
    public static final String ALGORITHM_P = "algorithm";

    /**
     * Description for parameter algorithm.
     */
    public static final String ALGORITHM_D = "<classname>classname of an algorithm implementing the interface " + Algorithm.class.getName() + ". Either full name to identify classpath or only classname, if its package is " + DEFAULT_ALGORITHM_PACKAGE + ".";

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
    public static final String DESCRIPTION_F = "description";

    /**
     * Description for description flag.
     */
    public static final String DESCRIPTION_D = "flag to obtain a description of any specified algorithm";

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
    public static final String DATABASE_CONNECTION_D = "<classname>classname of a class implementing the interface " + DatabaseConnection.class.getName() + ". Either full name to identify classpath or only classname, if its package is " + DEFAULT_DATABASE_CONNECTION_PACKAGE + ". (Default: " + DEFAULT_DATABASE_CONNECTION.getClass().getName() + ").";

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
    public static final String NORMALIZATION_D = "<class>a normalization (implementing " + Normalization.class.getName() + ") to use a database with normalized values";

    /**
     * Flag normalization undo.
     */
    public static final String NORMALIZATION_UNDO_F = "normUndo";

    /**
     * Description for flag normalization undo.
     */
    public static final String NORMALIZATION_UNDO_D = "flag to revert result to original values - invalid option if no normalization has been performed.";

    /**
     * The pattern to split for separate entries in a property string, which is
     * a &quot;,&quot;. TODO unification of properties
     */
    public static final Pattern PROPERTY_SEPARATOR = Pattern.compile(",");

    /**
     * The property key for available algorithms.
     */
    public static final String PROPERTY_ALGORITHMS = "ALGORITHMS";

    /**
     * The property key for available database connections.
     */
    public static final String PROPERTY_DATABASE_CONNECTIONS = "DATABASE_CONNECTIONS";

    /**
     * Properties for the KDDTask.
     */
    public static final Properties PROPERTIES;

    static
    {
        PROPERTIES = new Properties();
        String PROPERTIES_FILE = DEFAULT_ALGORITHM_PACKAGE.replace('.', File.separatorChar) + File.separatorChar + "KDDFramework.prp";
        try
        {
            PROPERTIES.load(ClassLoader.getSystemResourceAsStream(PROPERTIES_FILE));
        }
        catch(Exception e)
        {
            System.err.println("Warning: unable to load properties file " + PROPERTIES_FILE + ".");
        }
    }

    /**
     * The algorithm to run.
     */
    private Algorithm algorithm;

    /**
     * The database connection to have the algorithm run with.
     */
    private DatabaseConnection<MetricalObject> databaseConnection;

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
        parameterToDescription.put(ALGORITHM_P + OptionHandler.EXPECTS_VALUE, ALGORITHM_D);
        parameterToDescription.put(HELP_F, HELP_D);
        parameterToDescription.put(HELPLONG_F, HELP_D);
        parameterToDescription.put(DESCRIPTION_F, DESCRIPTION_D);
        parameterToDescription.put(DATABASE_CONNECTION_P + OptionHandler.EXPECTS_VALUE, DATABASE_CONNECTION_D);
        parameterToDescription.put(OUTPUT_P + OptionHandler.EXPECTS_VALUE, OUTPUT_D);
        parameterToDescription.put(NORMALIZATION_P + OptionHandler.EXPECTS_VALUE, NORMALIZATION_D);
        parameterToDescription.put(NORMALIZATION_UNDO_F, NORMALIZATION_UNDO_D);
        optionHandler = new OptionHandler(parameterToDescription, CALL);
    }

    /**
     * Returns a description for printing on command line interface.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage(""));
        description.append(NEWLINE);
        description.append("Subsequent options are firstly given to algorithm. Remaining parameters are given to databaseConnection.");
        description.append(NEWLINE);
        description.append(NEWLINE);
        description.append("Algorithms available within this framework:");
        description.append(NEWLINE);
        String algorithms = PROPERTIES.getProperty(PROPERTY_ALGORITHMS);
        String[] algorithmNames = algorithms != null ? PROPERTY_SEPARATOR.split(algorithms) : new String[0];
        for(int a = 0; a < algorithmNames.length; a++)
        {
            try
            {
                String desc = ((Algorithm) Class.forName(algorithmNames[a]).newInstance()).getDescription().toString();
                description.append(algorithmNames[a]);
                description.append(NEWLINE);
                description.append(desc);
                description.append(NEWLINE);
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
        }
        description.append(NEWLINE);
        description.append(NEWLINE);
        description.append("DatabaseConnections available within this framework:");
        description.append(NEWLINE);
        description.append(NEWLINE);
        String databaseConnections = PROPERTIES.getProperty(PROPERTY_DATABASE_CONNECTIONS);
        String[] databaseConnectionNames = databaseConnections != null ? PROPERTY_SEPARATOR.split(databaseConnections) : new String[0];
        for(int d = 0; d < databaseConnectionNames.length && !databaseConnectionNames[d].equals(""); d++)
        {
            try
            {
                String desc = ((DatabaseConnection) Class.forName(databaseConnectionNames[d]).newInstance()).description().toString();
                description.append(databaseConnectionNames[d]);
                description.append(NEWLINE);
                description.append(desc);
                description.append(NEWLINE);
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
        }
        description.append(NEWLINE);

        return description.toString();
    }

    /**
     * Sets the options accordingly to the specified list of parameters.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @SuppressWarnings("unchecked")
    public String[] setParameters(String[] args) throws IllegalArgumentException, AbortException
    {
        String[] remainingParameters = optionHandler.grabOptions(args);
        if(args.length == 0)
        {
            System.out.println("No options specified. Try flag -h to gain more information.");
            System.exit(0);
        }
        if(optionHandler.isSet(HELP_F) || optionHandler.isSet(HELPLONG_F))
        {
            throw new AbortException(description());
        }
        try
        {
            String name = optionHandler.getOptionValue(ALGORITHM_P);
            try
            {
                algorithm = (Algorithm) Class.forName(name).newInstance();
            }
            catch(ClassNotFoundException e)
            {
                algorithm = (Algorithm) Class.forName(DEFAULT_ALGORITHM_PACKAGE + "." + name).newInstance();
            }
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(NoParameterValueException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(InstantiationException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(IllegalAccessException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(ClassNotFoundException e)
        {
            throw new IllegalArgumentException(e);
        }
        if(optionHandler.isSet(DESCRIPTION_F))
        {
            throw new AbortException(algorithm.getDescription().toString() + '\n' + algorithm.description());
        }
        if(optionHandler.isSet(DATABASE_CONNECTION_P))
        {
            String name = optionHandler.getOptionValue(DATABASE_CONNECTION_P);
            try
            {
                try
                {
                    databaseConnection = (DatabaseConnection<MetricalObject>) Class.forName(name).newInstance();
                }
                catch(ClassNotFoundException e)
                {
                    databaseConnection = (DatabaseConnection<MetricalObject>) Class.forName(DEFAULT_DATABASE_CONNECTION_PACKAGE + "." + name).newInstance();
                }
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        else
        {
            try
            {
                databaseConnection = (DatabaseConnection<MetricalObject>) Class.forName(DEFAULT_DATABASE_CONNECTION).newInstance();
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        if(optionHandler.isSet(OUTPUT_P))
        {
            out = new File(optionHandler.getOptionValue(OUTPUT_P));
        }
        else
        {
            out = null;
        }
        if(optionHandler.isSet(NORMALIZATION_P))
        {
            String name = optionHandler.getOptionValue(NORMALIZATION_P);
            try
            {
                normalization = (Normalization) Class.forName(name).newInstance();
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e);
            }
            normalizationUndo = optionHandler.isSet(NORMALIZATION_UNDO_F);
        }
        else if(optionHandler.isSet(NORMALIZATION_UNDO_F))
        {
            throw new IllegalArgumentException("Illegal parameter setting: Flag " + NORMALIZATION_UNDO_F + " is set, but no normalization is specified.");
        }

        remainingParameters = algorithm.setParameters(remainingParameters);
        remainingParameters = databaseConnection.setParameters(remainingParameters);

        initialized = true;
        return remainingParameters;
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
    public void run() throws IllegalStateException
    {
        if(initialized)
        {
            algorithm.run(databaseConnection.getDatabase(normalization));
            try
            {
                if(normalizationUndo)
                {
                    algorithm.getResult().output(out, normalization);
                }
                else
                {
                    algorithm.getResult().output(out, null);
                }
            }
            catch(UnableToComplyException e)
            {
                throw new IllegalStateException("Error in restoring result to original values.", e);
            }
        }
        else
        {
            throw new IllegalStateException("KDD-Task was not properly initialized. Need to set parameters first.");
        }
    }

    /**
     * Runs a KDD task accordingly to the specified parameters.
     * 
     * @param args
     *            parameter list according to description
     */
    public static void main(String[] args)
    {
        KDDTask kddTask = new KDDTask();
        try
        {
            kddTask.setParameters(args);
            kddTask.run();
        }
        catch(AbortException e)
        {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        catch(IllegalArgumentException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        catch(IllegalStateException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

}
