package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * AbstractParser already provides the setting of the database according to
 * parameters.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractParser<T extends MetricalObject> implements Parser<T>
{
    /**
     * Option string for parameter database.
     */
    public static final String DATABASE_CLASS_P = "database";

    /**
     * Default value for parameter database.
     */
    public static final String DEFAULT_DATABASE = SequentialDatabase.class.getName();

    /**
     * Description for parameter database.
     */
    public static final String DATABASE_CLASS_D = "<classname>a class name specifying the database to be provided by the parse method (must implement " + Database.class.getName() + " - default: " + DEFAULT_DATABASE + ")";

    /**
     * The database.
     */
    protected Database<T> database;

    /**
     * OptionHandler for handling options.
     */
    private OptionHandler optionHandler;

    /**
     * AbstractParser already provides the setting of the database according to
     * parameters.
     */
    protected AbstractParser()
    {
        Map<String, String> parameterToDescription = new Hashtable<String, String>();
        parameterToDescription.put(DATABASE_CLASS_P + OptionHandler.EXPECTS_VALUE, DATABASE_CLASS_D);
        optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
    }

    /**
     * Returns a usage string based on the usage of optionHandler.
     * 
     * @param message
     *            a message string to be included in the usage string
     * @return a usage string based on the usage of optionHandler
     */
    protected String usage(String message)
    {
        return optionHandler.usage(message, false);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingOptions = optionHandler.grabOptions(args);
        if(optionHandler.isSet(DATABASE_CLASS_P))
        {
            database = Util.instantiate(Database.class,optionHandler.getOptionValue(DATABASE_CLASS_P));
        }
        else
        {
            database = Util.instantiate(Database.class,DEFAULT_DATABASE);
        }
        return database.setParameters(remainingOptions);
    }

    /**
     * Returns the parameter setting of the attributes.
     * 
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getParameterSettings()
    {
        List<AttributeSettings> result = new ArrayList<AttributeSettings>();

        AttributeSettings attributeSettings = new AttributeSettings(this);
        attributeSettings.addSetting(DATABASE_CLASS_P, database.getClass().getName());
        return result;
    }

}
