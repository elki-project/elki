package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.StandardLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

/**
 * Provides a database connection expecting input from standard in.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class InputStreamDatabaseConnection<T extends MetricalObject> implements DatabaseConnection<T>
{
    static
    {
        String PROPERTIES_FILE = DEFAULT_PACKAGE.replace('.', File.separatorChar) + File.separatorChar + "database.prp";
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
     * Default parser.
     */
    public final static Parser<RealVector> DEFAULT_PARSER = new StandardLabelParser();

    /**
     * Label for parameter parser.
     */
    public final static String PARSER_P = "parser";

    /**
     * Description of parameter parser.
     */
    public final static String PARSER_D = "<classname>a parser to provide a database (default: " + DEFAULT_PARSER.getClass().getName() + ")";

    /**
     * The parser.
     */
    protected Parser<T> parser;

    /**
     * The input to parse from.
     */
    protected InputStream in;

    /**
     * A map to provide parameters and description.
     */
    protected Map<String, String> parameterToDescription;
    
    /**
     * OptionHandler to handle options.
     */
    protected OptionHandler optionHandler;
    
    /**
     * Provides a database connection expecting input from standard in.
     *
     */
    @SuppressWarnings("unchecked")
    public InputStreamDatabaseConnection()
    {
        parameterToDescription = new Hashtable<String, String>();
        parameterToDescription.put(PARSER_P + OptionHandler.EXPECTS_VALUE, PARSER_D);
        try
        {
            parser = (Parser<T>) DEFAULT_PARSER.getClass().newInstance();
        }
        catch(InstantiationException e)
        {
            e.printStackTrace();
        }
        catch(IllegalAccessException e)
        {
            e.printStackTrace();
        }
        optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
    }

    /**
     * @see de.lmu.ifi.dbs.database.DatabaseConnection#getDatabase()
     */
    public Database<T> getDatabase()
    {
        return parser.parse(in);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    @SuppressWarnings("unchecked")
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("", false));
        description.append('\n');
        description.append("Parsers available within this framework for database connection "+this.getClass().getName()+":");
        description.append('\n');
        String parsers = PROPERTIES.getProperty(PROPERTY_PARSER);
        String[] parserNames = (parsers == null ? new String[0] : PROPERTY_SEPARATOR.split(parsers));
        for(int i = 0; i < parserNames.length; i++)
        {
            try
            {
                String desc = ((Parser<T>) Class.forName(parserNames[i]).newInstance()).description();
                description.append(parserNames[i]);
                description.append('\n');
                description.append(desc);
                description.append('\n');
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
        }

        description.append('\n');
        description.append("Databases available within this framework for database connection "+this.getClass().getName()+":");
        description.append('\n');
        String databases = PROPERTIES.getProperty(PROPERTY_DATABASE);
        String[] databaseNames = (databases == null ? new String[0] : PROPERTY_SEPARATOR.split(databases));
        for(int i = 0; i < databaseNames.length; i++)
        {
            try
            {
                String desc = ((Database<T>) Class.forName(databaseNames[i]).newInstance()).description();
                description.append(databaseNames[i]);
                description.append('\n');
                description.append(desc);
                description.append('\n');
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
        }

        return description.toString();
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingOptions = optionHandler.grabOptions(args);
        if(optionHandler.isSet(PARSER_P))
        {
            try
            {
                parser = (Parser<T>) Class.forName(optionHandler.getOptionValue(PARSER_P)).newInstance();
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        return parser.setParameters(remainingOptions);
    }

}
