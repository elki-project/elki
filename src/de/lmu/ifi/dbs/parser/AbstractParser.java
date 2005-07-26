package de.lmu.ifi.dbs.parser;

import java.util.Hashtable;
import java.util.Map;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractParser implements Parser
{
    public static final String DATABASE_CLASS_P = "database";
    
    public static final String DATABASE_CLASS_D = "<classname>a class name specifying the database to be provided by the parse method (must implement "+Database.class.getName()+")";

    private Class database;
    
    private OptionHandler optionHandler;
    
    protected AbstractParser()
    {
        Map<String,String> parameterToDescription = new Hashtable<String, String>();
        parameterToDescription.put(DATABASE_CLASS_P+OptionHandler.EXPECTS_VALUE,DATABASE_CLASS_D);
        optionHandler = new OptionHandler(parameterToDescription,"");
    }
    
    protected Database databaseInstance()
    {
        try
        {
            return (Database) database.newInstance();
        }
        catch(InstantiationException e)
        {
            return null;
        }
        catch(IllegalAccessException e)
        {
            return null;
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingOptions = optionHandler.grabOptions(args);
        if(optionHandler.isSet(DATABASE_CLASS_P))
        {
            try
            {
                Class<?> databaseClass = Class.forName(optionHandler.getOptionValue(DATABASE_CLASS_P));
                database = ((Database) databaseClass.newInstance()).getClass();                
            }
            catch(UnusedParameterException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(NoParameterValueException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        else
        {
            throw new IllegalArgumentException("Parser: Database is not specified.");
        }
        return remainingOptions;
    }

}
