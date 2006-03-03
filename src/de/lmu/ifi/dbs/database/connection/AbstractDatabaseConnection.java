package de.lmu.ifi.dbs.database.connection;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.parser.ObjectAndLabels;
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
   * Option string for parameter externalIDIndex.
   */
  public static final String EXTERNAL_ID_INDEX_P = "externalIDIndex";

  /**
   * Description for parameter classLabelIndex.
   */
  public static final String EXTERNAL_ID_INDEX_D = "<index>a positive integer specifiying the index of the label to be used as a external id.";

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
   * The index of the external id label, -1 if no external id label is specified.
   */
  private int externalIDIndex;

  /**
   * The index of the class label, -1 if no class label is specified.
   */
  protected int classLabelIndex;

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
   * True, if an external label needs to set. Default is false.
   */
  boolean forceExternalID = false;

  /**
   * AbstractDatabaseConnection already provides the setting of the database
   * according to parameters.
   */
  protected AbstractDatabaseConnection() {
    parameterToDescription.put(DATABASE_CLASS_P + OptionHandler.EXPECTS_VALUE, DATABASE_CLASS_D);
    parameterToDescription.put(CLASS_LABEL_INDEX_P + OptionHandler.EXPECTS_VALUE, CLASS_LABEL_INDEX_D);
    parameterToDescription.put(CLASS_LABEL_CLASS_P + OptionHandler.EXPECTS_VALUE, CLASS_LABEL_CLASS_D);
    parameterToDescription.put(EXTERNAL_ID_INDEX_P + OptionHandler.EXPECTS_VALUE, EXTERNAL_ID_INDEX_D);
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
    else if (optionHandler.isSet(CLASS_LABEL_CLASS_P)) {
      throw new IllegalArgumentException("Parameter " + CLASS_LABEL_INDEX_P + " must be specified!");
    }
    else classLabelIndex = -1;

    // external id label
    if (optionHandler.isSet(EXTERNAL_ID_INDEX_P)) {
      try {
        externalIDIndex = Integer.parseInt(optionHandler.getOptionValue(EXTERNAL_ID_INDEX_P)) - 1;
        if (externalIDIndex < 0) {
          throw new IllegalArgumentException("Parameter " + EXTERNAL_ID_INDEX_P + " has to be greater than 0!");
        }
        if (externalIDIndex == classLabelIndex) {
          throw new IllegalArgumentException("Parameters " + CLASS_LABEL_CLASS_P + " and " +
                                             EXTERNAL_ID_INDEX_P + " have equal values!");
        }
      }
      catch (NumberFormatException e) {
        IllegalArgumentException iae = new IllegalArgumentException(e);
        iae.fillInStackTrace();
        throw iae;
      }
    }
    else if (! forceExternalID) {
      externalIDIndex = -1;
    }
    else throw new IllegalArgumentException("Parameter " + EXTERNAL_ID_INDEX_P + " needs to be set!");

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
   * Normalizes and transforms the specified list of objects and their labels into a
   * list of objects and their associtaions.
   * @param objectAndLabelsList the list of object and their labels to be transformed
   * @param normalization the normalization to be applied
   * @return a list of normalized objects and their associations
   * @throws NonNumericFeaturesException if any exception occurs during normalization
   */
  protected List<ObjectAndAssociations<O>> normalizeAndTransformLabels(List<ObjectAndLabels<O>> objectAndLabelsList, Normalization<O> normalization) throws NonNumericFeaturesException {
    List<ObjectAndAssociations<O>> objectAndAssociationsList = transformLabels(objectAndLabelsList);

    if (normalization == null) {
      return objectAndAssociationsList;
    }
    else {
      return normalization.normalizeObjects(objectAndAssociationsList);
    }
  }

  /**
   * Transforms the specified list of objects and their labels into a
   * list of objects and their associtaions.
   *
   * @param objectAndLabelsList the list of object and their labels to be transformed
   * @return a list of objects and their associations
   */
  private List<ObjectAndAssociations<O>> transformLabels(List<ObjectAndLabels<O>> objectAndLabelsList) {
    List<ObjectAndAssociations<O>> result = new ArrayList<ObjectAndAssociations<O>>();

    for (ObjectAndLabels<O> objectAndLabels : objectAndLabelsList) {
      List<String> labels = objectAndLabels.getLabels();
      if (classLabelIndex > labels.size()) {
        throw new IllegalArgumentException("No class label at index " + (classLabelIndex + 1) + " specified!");
      }

      if (externalIDIndex > labels.size()) {
        throw new IllegalArgumentException("No external id label at index " + (externalIDIndex + 1) + " specified!");
      }

      String classLabel = null;
      String externalIDLabel = null;
      StringBuffer label = new StringBuffer();
      for (int i = 0; i < labels.size(); i++) {
        String l = labels.get(i);
        if (l.length() == 0) continue;

        if (i == classLabelIndex) {
          classLabel = l;
        }
        else if (i == externalIDIndex) {
          externalIDLabel = l;
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

      if (classLabel != null) {
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

      if (externalIDLabel != null) {
        associationMap.put(AssociationID.EXTERNAL_ID, externalIDLabel);
      }

      result.add(new ObjectAndAssociations<O>(objectAndLabels.getObject(), associationMap));
    }
    return result;
  }
}
