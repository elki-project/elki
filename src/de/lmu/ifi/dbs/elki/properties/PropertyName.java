package de.lmu.ifi.dbs.elki.properties;

import de.lmu.ifi.dbs.elki.utilities.ConstantObject;

import java.util.logging.Level;

/**
 * todo: remove old property names?
 * PropertyName for lookup in property file for class definitions.
 *
 * @author Arthur Zimek
 */
public final class PropertyName extends ConstantObject<PropertyName> {

  /**
   * Property debug level.
   */
  public static final PropertyName DEBUG_LEVEL = new PropertyName("DEBUG_LEVEL", Level.class);

  /**
   * Property algorithms.
   */
//  public static final PropertyName ALGORITHM = new PropertyName("ALGORITHMS", Algorithm.class);

  /**
   * Property DatabaseConnections.
   */
//  public static final PropertyName DATABASE_CONNECTIONS = new PropertyName("DATABASE_CONNECTIONS", DatabaseConnection.class);

  /**
   * Property Parser for InputStreamDatabaseConnections.
   */
//  public static final PropertyName INPUT_STREAM_DBC_PARSER = new PropertyName("INPUT_STREAM_DBC_PARSER", Parser.class);

  /**
   * Property Databases for InputStreamDatabaseConnections.
   */
//  public static final PropertyName INPUT_STREAM_DBC_DATABASE = new PropertyName("INPUT_STREAM_DBC_DATABASE", Database.class);

  /**
   * Property DistanceFunctions.
   */
//  public static final PropertyName DISTANCE_FUNCTIONS = new PropertyName("DISTANCE_FUNCTIONS", DistanceFunction.class);

  /**
   * Property Preprocessors for CorrelationDistanceFunction.
   */
//  public static final PropertyName CORRELATION_DISTANCE_FUNCTION_PREPROCESSOR = new PropertyName("CORRELATION_DISTANCE_FUNCTION_PREPROCESSOR", HiCOPreprocessor.class);

  /**
   * Property Preprocessors for LocallyWeightedDistanceFunction.
   */
//  public static final PropertyName LOCALLY_WEIGHTED_DISTANCE_FUNCTION_PREPROCESSOR = new PropertyName("LOCALLY_WEIGHTED_DISTANCE_FUNCTION_PREPROCESSOR", HiCOPreprocessor.class);

  /**
   * Serial version UID
   */
  private static final long serialVersionUID = 4157132118729322556L;

  /**
   * The type related to the property.
   */
  private Class<?> type;

  /**
   * Provides a new PropertyName of given name and type.
   * <p/>
   * All PropertyNames are unique w.r.t. their name.
   *
   * @param name the name of the PropertyName
   * @param type the type of the PropertyName
   */
  private PropertyName(final String name, final Class<?> type) {
    super(name);
    try {
      this.type = Class.forName(type.getName());
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Invalid class name \"" + type.getName() + "\" for property \"" + name + "\".");
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
   *                                on its own nor trying the default package as prefix
   */
  public Class<?> classForName(final String classname) throws ClassNotFoundException {
    try {
      return Class.forName(classname);
    }
    catch (ClassNotFoundException e) {
      Package defaultPackage = type.getPackage();
      return Class.forName(defaultPackage.getName() + "." + classname);
    }
  }

  /**
   * Returns the type of the PropertyName.
   *
   * @return the type of the PropertyName
   */
  public Class<?> getType() {
    try {
      return Class.forName(type.getName());
    }
    catch (ClassNotFoundException e) {
      throw new IllegalStateException("Invalid class name \"" + type.getName() + "\" for property \"" + this.getName() + "\".");
    }
  }

  /**
   * Gets the PropertyName for the given class
   * named as the classes name if it exists, creates and returns
   * it otherwise.
   *
   * @param type a class as type and the class' name as name
   * @return the PropertyName for the given class
   *         named as the classes name
   */
  public static PropertyName getOrCreatePropertyName(final Class<?> type) {
    PropertyName propertyName = getPropertyName(type.getName());
    if (propertyName == null) {
      propertyName = new PropertyName(type.getName(), type);
    }
    return propertyName;
  }

  /**
   * Returns the PropertyName for the given name
   * if it exists, null otherwise.
   *
   * @param name name of the desired PropertyName
   * @return the PropertyName for the given name
   */
  public static PropertyName getPropertyName(final String name) {
    return PropertyName.lookup(PropertyName.class, name);
  }
}
