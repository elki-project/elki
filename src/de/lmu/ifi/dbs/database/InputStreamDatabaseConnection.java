package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.parser.NormalizingParser;
import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.StandardLabelParser;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyDescription;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Provides a database connection expecting input from standard in.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class InputStreamDatabaseConnection<T extends MetricalObject> implements DatabaseConnection<T>
{
    /**
     * Prefix for properties related to InputStreamDatabaseConnection.
     */
    public final static String PREFIX = "INPUT_STREAM_DBC_"; 

    /**
     * Default parser.
     */
    public final static String DEFAULT_PARSER = StandardLabelParser.class.getName();

    /**
     * Label for parameter parser.
     */
    public final static String PARSER_P = "parser";

    /**
     * Description of parameter parser.
     */
    public final static String PARSER_D = "<classname>a parser to provide a database (default: " + DEFAULT_PARSER + ")";

    
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
     */
    @SuppressWarnings("unchecked")
    public InputStreamDatabaseConnection()
    {
        parameterToDescription = new Hashtable<String, String>();
        parameterToDescription.put(PARSER_P + OptionHandler.EXPECTS_VALUE, PARSER_D);
        try
        {
            parser = (Parser<T>) Class.forName(DEFAULT_PARSER).newInstance();
        }
        catch(InstantiationException e)
        {
            e.printStackTrace();
        }
        catch(IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
    }

    /**
     * @see de.lmu.ifi.dbs.database.DatabaseConnection#getDatabase(Normalization)
     */
    @SuppressWarnings("unchecked")
    public Database<T> getDatabase(Normalization normalization)
    {
        if(normalization != null)
        {
            if(parser instanceof NormalizingParser)
            {
                ((NormalizingParser) parser).setNormalization(normalization);
            }
            else
            {
                throw new UnsupportedOperationException("Parser " + parser.getClass().getName() + " is not able to perform a normalization.");
            }
        }
        return parser.parse(in);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("", false));
        description.append('\n');
        description.append("Parsers available within this framework for database connection ");
        description.append(this.getClass().getName());
        description.append(":");
        description.append('\n');
        for(PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix()+PROPERTY_PARSER)))
        {
            description.append(pd.getEntry());
            description.append('\n');
            description.append(pd.getDescription());
            description.append('\n');
        }
        description.append('\n');
        description.append("Databases available within this framework for database connection ");
        description.append(this.getClass().getName()); 
        description.append(":");
        description.append('\n');
        for(PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix()+PROPERTY_DATABASE)))
        {
            description.append(pd.getEntry());
            description.append('\n');
            description.append(pd.getDescription());
            description.append('\n');
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

    /**
     * Returns the parameter setting of the attributes.
     * 
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> result = new ArrayList<AttributeSettings>();

        AttributeSettings attributeSettings = new AttributeSettings(this);
        attributeSettings.addSetting(PARSER_P, parser.getClass().toString());

        result.add(attributeSettings);
        return result;
    }

    /**
     * Returns the prefix for properties concerning InputStreamDatabaseConnection.
     * Extending classes requiring other properties should overwrite this method
     * to provide another prefix.
     */
    protected String propertyPrefix()
    {
        return PREFIX;
    }
    
}
