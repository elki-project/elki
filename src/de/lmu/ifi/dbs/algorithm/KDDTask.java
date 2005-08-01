package de.lmu.ifi.dbs.algorithm;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.database.DatabaseConnection;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

/**
 * TODO comment
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KDDTask implements Parameterizable
{
    private static final String CALL = "java "+KDDTask.class.getName();
    
    public static final String NEWLINE = System.getProperty("line.separator");
    
    public static final String DEFAULT_PACKAGE = KDDTask.class.getPackage().getName();
    
    public static final String DEFAULT_DATABASE_CONNECTION_PACKAGE = DatabaseConnection.class.getPackage().getName();
    
    public static final String ALGORITHM_P = "algorithm";
    
    public static final String ALGORITHM_D = "<classname>classname of an algorithm implementing the interface "+Algorithm.class.getName()+". Either full name to identify classpath or only classname, if its package is "+DEFAULT_PACKAGE+"."; 
    
    public static final String HELP_F = "h";
    
    public static final String HELPLONG_F = "help";
    
    public static final String HELP_D = "flag to obtain help-message, either for the main-routine or for any specified algorithm. Causes immediate stop of the program.";

    public static final String DESCRIPTION_F = "description";
    
    public static final String DESCRIPTION_D = "flag to obtain a description of any specified algorithm";
    
    private static final DatabaseConnection DEFAULT_DATABASE_CONNECTION = new FileBasedDatabaseConnection(); 
    
    public static final String DATABASE_CONNECTION_P = "dbc";
    
    public static final String DATABASE_CONNECTION_D = "<classname>classname of a class implementing the interface "+DatabaseConnection.class.getName()+". Either full name to identify classpath or only classname, if its package is "+DEFAULT_DATABASE_CONNECTION_PACKAGE+". (Default: "+DEFAULT_DATABASE_CONNECTION.getClass().getName()+").";
    
    public static final String OUTPUT_P = "out";
    
    public static final String OUTPUT_D = "<filename>file to write the obtained results in. If an algorithm requires several outputfiles, the given filename will be used as prefix followed by automatically created markers. If this parameter is omitted, per default the output will sequentially be given to STDOUT.";
    
    public static final Properties PROPERTIES;
    public static final Pattern PROPERTY_SEPARATOR = Pattern.compile(",");
    public static final String PROPERTY_ALGORITHMS = "ALGORITHMS";
    public static final String PROPERTY_DATABASE_CONNECTIONS = "DATABASE_CONNECTIONS";
    static
    {
        PROPERTIES = new Properties();
        String PROPERTIES_FILE = DEFAULT_PACKAGE.replace('.',File.separatorChar)+File.separatorChar+"KDDFramework.prp";        
        try
        {
            PROPERTIES.load(ClassLoader.getSystemResourceAsStream(PROPERTIES_FILE));
        }
        catch(Exception e)
        {
            System.err.println("Warning: unable to load properties file "+PROPERTIES_FILE+".");
        }        
    }
    
    private Algorithm algorithm;
    
    private DatabaseConnection databaseConnection;
    
    private File out;
    
    private boolean initialized = false;

    private OptionHandler optionHandler;

    /**
     * TODO
     *
     */
    public KDDTask()
    {
        Map<String,String> parameterToDescription = new Hashtable<String,String>();
        parameterToDescription.put(ALGORITHM_P+OptionHandler.EXPECTS_VALUE,ALGORITHM_D);
        parameterToDescription.put(HELP_F,HELP_D);
        parameterToDescription.put(HELPLONG_F,HELP_D);
        parameterToDescription.put(DESCRIPTION_F,DESCRIPTION_D);
        parameterToDescription.put(DATABASE_CONNECTION_P+OptionHandler.EXPECTS_VALUE,DATABASE_CONNECTION_D);
        parameterToDescription.put(OUTPUT_P+OptionHandler.EXPECTS_VALUE,OUTPUT_D);
        optionHandler = new OptionHandler(parameterToDescription,CALL);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage(""));
        description.append(NEWLINE);
        description.append("Subsequent options are firstly given to algorithm, secondly to databaseConnection.");
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
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
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
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
        }
        description.append(NEWLINE);
        
        return description.toString();
    }


    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException, AbortException 
    {
        String[] remainingParameters = optionHandler.grabOptions(args);
        if(args.length==0)
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
                algorithm = (Algorithm) Class.forName(DEFAULT_PACKAGE+"."+name).newInstance();
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
            throw new AbortException(algorithm.description());
        }
        if(optionHandler.isSet(DATABASE_CONNECTION_P))
        {
            String name = optionHandler.getOptionValue(DATABASE_CONNECTION_P);
            try
            {
                try
                {
                    databaseConnection = (DatabaseConnection) Class.forName(name).newInstance();
                }
                catch(ClassNotFoundException e)
                {
                    databaseConnection = (DatabaseConnection) Class.forName(DEFAULT_DATABASE_CONNECTION_PACKAGE+"."+name).newInstance();
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
            databaseConnection = DEFAULT_DATABASE_CONNECTION;
        }
        remainingParameters = algorithm.setParameters(remainingParameters);
        remainingParameters = databaseConnection.setParameters(remainingParameters);
        if(optionHandler.isSet(OUTPUT_P))
        {
            out = new File(optionHandler.getOptionValue(OUTPUT_P));
        }
        else
        {
            out = null;
        }
        initialized = true;
        return remainingParameters;
    }
    
    /**
     * TODO
     * 
     * 
     * @throws IllegalStateException
     */
    public void run() throws IllegalStateException
    {
        if(initialized)
        {
            algorithm.run(databaseConnection.getDatabase());
            algorithm.getResult().output(out);
        }
        else
        {
            throw new IllegalStateException("KDD-Task was not properly initialized. Need to set parameters first.");
        }
    }

    /**
     * TODO
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        KDDTask kddTask = new KDDTask();
        try
        {
            kddTask.setParameters(args);
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
        kddTask.run();
    }

}
