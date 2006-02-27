package de.lmu.ifi.dbs.database.connection;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.AssociationID;
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
 * Abstract super class for all database connections. AbstractDatabaseConnection
 * already provides the setting of the database according to parameters.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract public class AbstractDatabaseConnection<O extends DatabaseObject> implements DatabaseConnection<O> {
  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

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
  public static final String DATABASE_CLASS_D = "<class>a class name specifying the database to be provided by the parse method (must implement " + Database.class.getName() + " - default: " + DEFAULT_DATABASE + ")";

  /**
   * Option string for parameter classLabelIndex.
   */
  public static final String CLASS_LABEL_INDEX_P = "classLabelIndex";

  /**
   * Description for parameter classLabelIndex.
   */
  public static final String CLASS_LABEL_INDEX_D = "<index>a positive integer specifiying the index of the label to be used as class label.";

  /**
   * Option string for parameter classLabelClass.
   */
  public static final String CLASS_LABEL_CLASS_P = "classLabelClass";

  /**
   * Description for parameter classLabelClass.
   */
  public static final String CLASS_LABEL_CLASS_D = "<class>a class name extending " + ClassLabel.class.getName() + " as association of occuring class labels. Default: association of labels as simple label.";

  /**
   * The index of the class label, -1 if no class label is specified.
   */
  private int classLabelIndex;

  /**
   * The class name for a class label.
   */
  private String classLabelClass;

  /**
   * The database.
   */
  Database<O> database;

  /**
   * OptionHandler for handling options.
   */
  OptionHandler optionHandler;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * AbstractDatabaseConnection already provides the setting of the database
   * according to parameters.
   */
  protected AbstractDatabaseConnection() {
    parameterToDescription.put(DATABASE_CLASS_P + OptionHandler.EXPECTS_VALUE, DATABASE_CLASS_D);
    parameterToDescription.put(CLASS_LABEL_INDEX_P + OptionHandler.EXPECTS_VALUE, CLASS_LABEL_INDEX_D);
    parameterToDescription.put(CLASS_LABEL_CLASS_P + OptionHandler.EXPECTS_VALUE, CLASS_LABEL_CLASS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    // database
    if (optionHandler.isSet(DATABASE_CLASS_P)) {
      database = Util.instantiate(Database.class, optionHandler.getOptionValue(DATABASE_CLASS_P));
    }
    else {
      database = Util.instantiate(Database.class, DEFAULT_DATABASE);
    }

    // class label
    if (optionHandler.isSet(CLASS_LABEL_INDEX_P)) {
      try {
        classLabelIndex = Integer.parseInt(optionHandler.getOptionValue(CLASS_LABEL_INDEX_P)) - 1;
        if (classLabelIndex < 0) {
          throw new IllegalArgumentException("Parameter " + CLASS_LABEL_INDEX_P + " has to be greater than 0!");
        }
      }
      catch (NumberFormatException e) {
        IllegalArgumentException iae = new IllegalArgumentException(e);
        iae.fillInStackTrace();
        throw iae;
      }

      if (optionHandler.isSet(CLASS_LABEL_CLASS_P)) {
        classLabelClass = optionHandler.getOptionValue(CLASS_LABEL_CLASS_P);
        try {
          ClassLabel.class.cast(Class.forName(classLabelClass).newInstance());
        }
        catch (InstantiationException e) {
          IllegalArgumentException iae = new IllegalArgumentException(e);
          iae.fillInStackTrace();
          throw iae;
        }
        catch (IllegalAccessException e) {
          IllegalArgumentException iae = new IllegalArgumentException(e);
          iae.fillInStackTrace();
          throw iae;
        }
        catch (ClassNotFoundException e) {
          IllegalArgumentException iae = new IllegalArgumentException(e);
          iae.fillInStackTrace();
          throw iae;
        }
      }
      else classLabelClass = SimpleClassLabel.class.getName();
    }
    else classLabelIndex = -1;

    return database.setParameters(remainingParameters);
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
    if (classLabelClass != null) {
      attributeSettings.addSetting(CLASS_LABEL_INDEX_P, classLabelClass);
    }
    result.add(attributeSettings);
    return result;
  }

  /**
   * Transforms the specified labelList into a map of association id an
   * association object suitable for inserting objects into the database
   *
   * @param labelList the list to be transformes
   * @return a map of association id an association object
   */
  protected List<Map<AssociationID, Object>> transformLabels(List<List<String>> labelList) {
    List<Map<AssociationID, Object>> result = new ArrayList<Map<AssociationID, Object>>();

    for (List<String> labels : labelList) {
      if (classLabelIndex > labels.size()) {
        throw new IllegalArgumentException("No label at index " + + classLabelIndex + " specified!");
      }

      String classLabel = null;
      StringBuffer label = new StringBuffer();
      for (int i = 0; i < labels.size(); i++) {
        String l = labels.get(i);
        if (l.length() == 0) continue;

        if (i == classLabelIndex) {
          classLabel = l;
        }
        else {
          if (label.length() == 0) {
            label.append(l);
          }
          else {
            label.append(LABEL_CONCATENATION);
            label.append(l);
          }
        }
      }

      Map<AssociationID, Object> associationMap = new Hashtable<AssociationID, Object>();
      associationMap.put(AssociationID.LABEL, label.toString());

      if (classLabelClass != null) {
        try {
          Object classLabelAssociation = Class.forName(classLabelClass).newInstance();
          ((ClassLabel) classLabelAssociation).init(classLabel);
          associationMap.put(AssociationID.CLASS, classLabelAssociation);
        }
        catch (InstantiationException e) {
          IllegalStateException ise = new IllegalStateException(e);
          ise.fillInStackTrace();
          throw ise;
        }
        catch (IllegalAccessException e) {
          IllegalStateException ise = new IllegalStateException(e);
          ise.fillInStackTrace();
          throw ise;
        }
        catch (ClassNotFoundException e) {
          IllegalStateException ise = new IllegalStateException(e);
          ise.fillInStackTrace();
          throw ise;
        }
      }
      result.add(associationMap);
    }
    return result;
  }
}
