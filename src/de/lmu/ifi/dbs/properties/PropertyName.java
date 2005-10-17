package de.lmu.ifi.dbs.properties;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.DatabaseConnection;
import de.lmu.ifi.dbs.database.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.distance.CorrelationDistanceFunction;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.ConstantObject;

/**
 * PropertyName for lookup in property file for class definitions.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public final class PropertyName extends ConstantObject
{
    /**
     * Property algorithms.
     */
    public static final PropertyName ALGORITHM = new PropertyName("ALGORITHMS",Algorithm.class);
    
    /**
     * Property DatabaseConnections.
     */
    public static final PropertyName DATABASE_CONNECTIONS = new PropertyName("DATABASE_CONNECTIONS",DatabaseConnection.class);
    
    /**
     * Property Parser for InputStreamDatabaseConnections.
     */
    public static final PropertyName INPUT_STREAM_DBC_PARSER = new PropertyName(InputStreamDatabaseConnection.PREFIX+DatabaseConnection.PROPERTY_PARSER,Parser.class);
    
    /**
     * Property Databases for InputStreamDatabaseConnections.
     */
    public static final PropertyName INPUT_STREAM_DBC_DATABASE = new PropertyName(InputStreamDatabaseConnection.PREFIX+DatabaseConnection.PROPERTY_DATABASE,Database.class);
    
    /**
     * Property DistanceFunctions.
     */
    public static final PropertyName DISTANCE_FUNCTIONS = new PropertyName("DISTANCE_FUNCTIONS",DistanceFunction.class);
    
    /**
     * Property Preprocessors for CorrelationDistanceFunction.
     */
    public static final PropertyName CORRELATION_DISTANCE_FUNCTION_PREPROCESSOR = new PropertyName(CorrelationDistanceFunction.PREFIX+CorrelationDistanceFunction.PROPERTY_PREPROCESSOR,CorrelationDimensionPreprocessor.class);
    
    /**
     * Property Preprocessors for LocallyWeightedDistanceFunction.
     */
    public static final PropertyName LOCALLY_WEIGHTED_DISTANCE_FUNCTION_PREPROCESSOR = new PropertyName(LocallyWeightedDistanceFunction.PREFIX+LocallyWeightedDistanceFunction.PROPERTY_PREPROCESSOR,CorrelationDimensionPreprocessor.class);
    
    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 4157132118729322556L;
    
    /**
     * The type related to the property.
     */
    private Class type;
    
    /**
     * Provides a new PropertyName of given name and type.
     * 
     * @param name the name of the PropertyName
     * @param type the tape of the PropertyName
     */
    private PropertyName(final String name, final Class type)
    {
        super(name);
        try
        {
            this.type = Class.forName(type.getName());
        }
        catch(ClassNotFoundException e)
        {
            throw new IllegalArgumentException("Invalid class name \""+type.getName()+"\" for property \""+name+"\".");
        }
    }
    
    /**
     * Returns a class for the given name.
     * If the name is not found, as package the default package defined by this PropertyName
     * is tried. If this fails, the method throws a ClassNotFoundException.
     * 
     * @param classname name of the class to return
     * @return a class for the given name
     * @throws ClassNotFoundException if the class for the given name is found neither
     * on its own nor trying the default package as prefix
     */
    public Class classForName(String classname) throws ClassNotFoundException
    {
        try
        {
            return Class.forName(classname);
        }
        catch(ClassNotFoundException e)
        {
            Package defaultPackage = type.getPackage();
            return Class.forName(defaultPackage.getName() + "." + classname);
        }
    }
    
    /**
     * Returns the type of the PropertyName.
     * 
     * 
     * @return the type of the PropertyName
     */
    public Class getType()
    {
        try
        {
            return Class.forName(type.getName());
        }
        catch(ClassNotFoundException e)
        {
            throw new IllegalStateException("Invalid class name \""+type.getName()+"\" for property \""+this.getName()+"\".");
        }
    }
    
    /**
     * Returns the PropertyName for the given name.
     * 
     * 
     * @param name name of the desired PropertyName
     * @return the PropertyName for the given name 
     */
    public static PropertyName getPropertyName(String name)
    {
        return (PropertyName) PropertyName.lookup(PropertyName.class,name);
    }
}
