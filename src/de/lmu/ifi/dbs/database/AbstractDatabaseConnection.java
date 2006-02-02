package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Hashtable;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Abstract super class for all database connections.
 * AbstractDatabaseConnection already provides the setting of the database according to
 * parameters.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract public class AbstractDatabaseConnection<M extends MetricalObject> implements DatabaseConnection<M> {
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
   * Option string for parameter association.
   */
  public static final String ASSOCIATION_P = "association";

  /**
   * Description for parameter association.
   */
  public static final String ASSOCIATION_D = "<ClassLabel>a class name extending " + ClassLabel.class.getName() + " as association of occuring labels. Default: association of labels as simple label.";

  /**
   * The association id for a label.
   */
  AssociationID associationID = AssociationID.LABEL;

  /**
   * The class name for a label.
   */
  String classLabel;

  /**
   * The database.
   */
  Database<M> database;

  /**
   * OptionHandler for handling options.
   */
  OptionHandler optionHandler;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * AbstractDatabaseConnection already provides the setting of the database according to
   * parameters.
   */
  protected AbstractDatabaseConnection() {
    parameterToDescription.put(DATABASE_CLASS_P + OptionHandler.EXPECTS_VALUE, DATABASE_CLASS_D);
    parameterToDescription.put(ASSOCIATION_P + OptionHandler.EXPECTS_VALUE, ASSOCIATION_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingOptions = optionHandler.grabOptions(args);

    if (optionHandler.isSet(DATABASE_CLASS_P)) {
      database = Util.instantiate(Database.class, optionHandler.getOptionValue(DATABASE_CLASS_P));
    }
    else {
      database = Util.instantiate(Database.class, DEFAULT_DATABASE);
    }
    if (optionHandler.isSet(ASSOCIATION_P)) {
      classLabel = optionHandler.getOptionValue(ASSOCIATION_P);
      try {
        ClassLabel.class.cast(Class.forName(classLabel).newInstance());
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e);
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e);
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
      associationID = AssociationID.CLASS;
    }
    return database.setParameters(remainingOptions);
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = new ArrayList<AttributeSettings>();

    AttributeSettings attributeSettings = new AttributeSettings(this);
    attributeSettings.addSetting(DATABASE_CLASS_P, database.getClass().getName());
    attributeSettings.addSetting(ASSOCIATION_P, classLabel.getClass().getName());
    return result;
  }
}
