package de.lmu.ifi.dbs.elki.database.connection;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.SequentialDatabase;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.NotEqualValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Abstract super class for all database connections. AbstractDatabaseConnection
 * already provides the setting of the database according to parameters.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be provided by the implementing
 *        class as element of the supplied database
 */
public abstract class AbstractDatabaseConnection<O extends DatabaseObject> extends AbstractLoggable implements DatabaseConnection<O> {
  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

  /**
   * OptionID for {@link #DATABASE_PARAM}
   */
  public static final OptionID DATABASE_ID = OptionID.getOrCreateOptionID("dbc.database", "Database class to be provided by the parse method.");

  /**
   * Parameter to specify the database to be provided by the parse method, must
   * extend {@link Database}.
   * <p>
   * Default value: {@link SequentialDatabase}
   * </p>
   * <p>
   * Key: {@code -dbc.database}
   * </p>
   */
  private final ObjectParameter<Database<O>> DATABASE_PARAM = new ObjectParameter<Database<O>>(DATABASE_ID, Database.class, SequentialDatabase.class);

  /**
   * Holds the instance of the database specified by {@link #DATABASE_PARAM}.
   */
  Database<O> database;

  /**
   * OptionID for {@link #CLASS_LABEL_INDEX_PARAM}
   */
  public static final OptionID CLASS_LABEL_INDEX_ID = OptionID.getOrCreateOptionID("dbc.classLabelIndex", "The index of the label to be used as class label.");

  /**
   * Optional parameter that specifies the index of the label to be used as
   * class label, must be an integer equal to or greater than 0.
   * <p>
   * Key: {@code -dbc.classLabelIndex}
   * </p>
   */
  private final IntParameter CLASS_LABEL_INDEX_PARAM = new IntParameter(CLASS_LABEL_INDEX_ID, new GreaterEqualConstraint(0), true);

  /**
   * Holds the value of {@link #CLASS_LABEL_INDEX_PARAM}, null if no class label
   * is specified.
   */
  protected Integer classLabelIndex;

  /**
   * OptionID for {@link #CLASS_LABEL_CLASS_PARAM}
   */
  public static final OptionID CLASS_LABEL_CLASS_ID = OptionID.getOrCreateOptionID("dbc.classLabelClass", "Class label class to use.");

  /**
   * Parameter to specify the association of occurring class labels, must extend
   * {@link ClassLabel}.
   * <p>
   * Default value: {@link SimpleClassLabel}
   * </p>
   * <p>
   * Key: {@code -dbc.classLabelClass}
   * </p>
   */
  private final ObjectParameter<ClassLabel> CLASS_LABEL_CLASS_PARAM = new ObjectParameter<ClassLabel>(CLASS_LABEL_CLASS_ID, ClassLabel.class, SimpleClassLabel.class);

  /**
   * Holds the value of {@link #CLASS_LABEL_CLASS_PARAM}.
   */
  private Class<? extends ClassLabel> classLabelClass;

  /**
   * OptionID for {@link #EXTERNAL_ID_INDEX_PARAM}
   */
  public static final OptionID EXTERNAL_ID_INDEX_ID = OptionID.getOrCreateOptionID("dbc.externalIDIndex", "The index of the label to be used as an external id.");

  /**
   * Optional parameter that specifies the index of the label to be used as an
   * external id, must be an integer equal to or greater than 0.
   * <p>
   * Key: {@code -dbc.externalIDIndex}
   * </p>
   */
  private final IntParameter EXTERNAL_ID_INDEX_PARAM = new IntParameter(EXTERNAL_ID_INDEX_ID, new GreaterEqualConstraint(0), true);

  /**
   * Holds the value of {@link #EXTERNAL_ID_INDEX_PARAM}.
   */
  private Integer externalIDIndex;

  /**
   * Adds parameters {@link #DATABASE_PARAM}, {@link #CLASS_LABEL_INDEX_PARAM},
   * {@link #CLASS_LABEL_CLASS_PARAM}, and {@link #EXTERNAL_ID_INDEX_PARAM}, to
   * the option handler additionally to parameters of super class.
   */
  protected AbstractDatabaseConnection(Parameterization config, boolean forceExternalID) {
    super();

    // parameter database
    if (config.grab(this, DATABASE_PARAM)) {
      database = DATABASE_PARAM.instantiateClass(config);
    }

    // parameter class label index
    config.grab(this, CLASS_LABEL_INDEX_PARAM);
    config.grab(this, CLASS_LABEL_CLASS_PARAM);
    if(CLASS_LABEL_INDEX_PARAM.isSet()) {
      classLabelIndex = CLASS_LABEL_INDEX_PARAM.getValue();
      classLabelClass = CLASS_LABEL_CLASS_PARAM.getValue();
    }

    // parameter external ID index
    if (forceExternalID) {
      EXTERNAL_ID_INDEX_PARAM.setOptional(false);
    }
    config.grab(this, EXTERNAL_ID_INDEX_PARAM);
    if(EXTERNAL_ID_INDEX_PARAM.isSet()) {
      externalIDIndex = EXTERNAL_ID_INDEX_PARAM.getValue();
    }

    // global parameter constraints
    ArrayList<NumberParameter<Integer>> globalConstraints = new ArrayList<NumberParameter<Integer>>();
    globalConstraints.add(CLASS_LABEL_INDEX_PARAM);
    globalConstraints.add(EXTERNAL_ID_INDEX_PARAM);
    config.checkConstraint(new NotEqualValueGlobalConstraint<Integer>(globalConstraints));
  }

  /**
   * Normalizes and transforms the specified list of objects and their labels
   * into a list of objects and their associations.
   * 
   * @param objectAndLabelsList the list of object and their labels to be
   *        transformed
   * @param normalization the normalization to be applied
   * @return a list of normalized objects and their associations
   * @throws NonNumericFeaturesException if any exception occurs during
   *         normalization
   */
  protected List<Pair<O, Associations>> normalizeAndTransformLabels(List<Pair<O, List<String>>> objectAndLabelsList, Normalization<O> normalization) throws NonNumericFeaturesException {
    List<Pair<O, Associations>> objectAndAssociationsList = transformLabels(objectAndLabelsList);

    if(normalization == null) {
      return objectAndAssociationsList;
    }
    else {
      return normalization.normalizeObjects(objectAndAssociationsList);
    }
  }

  /**
   * Transforms the specified list of objects and their labels into a list of
   * objects and their associations.
   * 
   * @param objectAndLabelsList the list of object and their labels to be
   *        transformed
   * @return a list of objects and their associations
   */
  private List<Pair<O, Associations>> transformLabels(List<Pair<O, List<String>>> objectAndLabelsList) {
    List<Pair<O, Associations>> result = new ArrayList<Pair<O, Associations>>();

    for(Pair<O, List<String>> objectAndLabels : objectAndLabelsList) {
      List<String> labels = objectAndLabels.getSecond();
      if(classLabelIndex != null && classLabelIndex >= labels.size()) {
        throw new IllegalArgumentException("No class label at index " + (classLabelIndex) + " specified!");
      }

      if(externalIDIndex != null && externalIDIndex >= labels.size()) {
        throw new IllegalArgumentException("No external id label at index " + (externalIDIndex) + " specified!");
      }

      String classLabel = null;
      String externalIDLabel = null;
      StringBuffer label = new StringBuffer();
      for(int i = 0; i < labels.size(); i++) {
        String l = labels.get(i).trim();
        if(l.length() == 0) {
          continue;
        }

        if(classLabelIndex != null && i == classLabelIndex) {
          classLabel = l;
        }
        else if(externalIDIndex != null && i == externalIDIndex) {
          externalIDLabel = l;
        }
        else {
          if(label.length() == 0) {
            label.append(l);
          }
          else {
            label.append(LABEL_CONCATENATION);
            label.append(l);
          }
        }
      }

      Associations associationMap = new Associations();
      if(label.length() != 0) {
        associationMap.put(AssociationID.LABEL, label.toString());
      }

      if(classLabel != null) {
        try {
          ClassLabel classLabelAssociation = classLabelClass.newInstance();
          classLabelAssociation.init(classLabel);
          associationMap.put(AssociationID.CLASS, classLabelAssociation);
        }
        catch(InstantiationException e) {
          IllegalStateException ise = new IllegalStateException(e);
          throw ise;
        }
        catch(IllegalAccessException e) {
          IllegalStateException ise = new IllegalStateException(e);
          throw ise;
        }
      }

      if(externalIDLabel != null) {
        associationMap.put(AssociationID.EXTERNAL_ID, externalIDLabel);
      }

      result.add(new Pair<O, Associations>(objectAndLabels.getFirst(), associationMap));
    }
    return result;
  }
}