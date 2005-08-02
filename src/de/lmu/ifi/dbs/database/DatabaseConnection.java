package de.lmu.ifi.dbs.database;

import java.util.Properties;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * DatabaseConnection is to provide a database.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface DatabaseConnection extends Parameterizable
{
    /**
     * Properties for DatabaseConnections.
     */
    public static final Properties PROPERTIES = new Properties();
    
    /**
     * The default package for databases and database connections.
     */
    public static final String DEFAULT_PACKAGE = DatabaseConnection.class.getPackage().getName();
    
    /**
     * The pattern to split for separate entries in a property string,
     * which is a &quot;,&quot;.
     */
    public static final Pattern PROPERTY_SEPARATOR = Pattern.compile(",");
    
    /**
     * Property key for available parsers.
     */
    public static final String PROPERTY_PARSER = "PARSER";
    
    /**
     * Property key for available databases.
     */
    public static final String PROPERTY_DATABASE = "DATABASE";
    
    /**
     * Returns a Database according to parameter settings.
     * 
     * 
     * @return a Database according to parameter settings
     */
    Database getDatabase();
}
