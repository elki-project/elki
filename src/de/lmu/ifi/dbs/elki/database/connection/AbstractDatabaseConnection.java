package de.lmu.ifi.dbs.elki.database.connection;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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
public abstract class AbstractDatabaseConnection<O extends DatabaseObject> implements DatabaseConnection<O> {
  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

  /**
   * Parameter to specify the database to be provided by the parse method.
   * <p>
   * Key: {@code -dbc.database}
   * </p>
   */
  public static final OptionID DATABASE_ID = OptionID.getOrCreateOptionID("dbc.database", "Database class to be provided by the parse method.");

  /**
   * The database provided by the parse method.
   */
  Database<O> database;

  /**
   * Optional parameter that specifies the index of the label to be used as
   * class label, must be an integer equal to or greater than 0.
   * <p>
   * Key: {@code -dbc.classLabelIndex}
   * </p>
   */
  public static final OptionID CLASS_LABEL_INDEX_ID = OptionID.getOrCreateOptionID("dbc.classLabelIndex", "The index of the label to be used as class label.");

  /**
   * The index of the label to be used as class label, null if no class label is
   * specified.
   */
  protected Integer classLabelIndex;

  /**
   * Parameter to specify the class of occurring class labels.
   * <p>
   * Key: {@code -dbc.classLabelClass}
   * </p>
   */
  public static final OptionID CLASS_LABEL_CLASS_ID = OptionID.getOrCreateOptionID("dbc.classLabelClass", "Class label class to use.");

  /**
   * The class label class to use.
   */
  private Class<? extends ClassLabel> classLabelClass;

  /**
   * Factory method for getting parameters.
   * 
   * @param config Parameterization
   * @return parameters
   */
  public static <O extends DatabaseObject> Parameters<O> getParameters(Parameterization config) {
    // parameter database
    final ObjectParameter<Database<O>> dbParam = new ObjectParameter<Database<O>>(DATABASE_ID, Database.class, HashmapDatabase.class);
    Database<O> database = config.grab(dbParam) ? dbParam.instantiateClass(config) : null;

    // parameter class label index
    final IntParameter classLabelIndexParam = new IntParameter(CLASS_LABEL_INDEX_ID, new GreaterEqualConstraint(0), true);
    final ObjectParameter<ClassLabel> classlabelClassParam = new ObjectParameter<ClassLabel>(CLASS_LABEL_CLASS_ID, ClassLabel.class, SimpleClassLabel.class);

    Integer classLabelIndex = null;
    Class<? extends ClassLabel> classLabelClass = null;

    config.grab(classLabelIndexParam);
    config.grab(classlabelClassParam);
    if(classLabelIndexParam.isDefined() && classlabelClassParam.isDefined()) {
      classLabelIndex = classLabelIndexParam.getValue();
      classLabelClass = classlabelClassParam.getValue();
    }

    return new Parameters<O>(database, classLabelIndex, classLabelClass);
  }

  /**
   * Constructor.
   * 
   * @param database the instance of the database
   * @param classLabelIndex the index of the label to be used as class label,
   *        can be null
   * @param classLabelClass the association of occurring class labels
   */
  protected AbstractDatabaseConnection(Database<O> database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass) {
    this.database = database;
    this.classLabelIndex = classLabelIndex;
    this.classLabelClass = classLabelClass;
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
  protected List<Pair<O, DatabaseObjectMetadata>> normalizeAndTransformLabels(List<Pair<O, List<String>>> objectAndLabelsList, Normalization<O> normalization) throws NonNumericFeaturesException {
    List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList = transformLabels(objectAndLabelsList);

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
  private List<Pair<O, DatabaseObjectMetadata>> transformLabels(List<Pair<O, List<String>>> objectAndLabelsList) {
    List<Pair<O, DatabaseObjectMetadata>> result = new ArrayList<Pair<O, DatabaseObjectMetadata>>();

    for(Pair<O, List<String>> objectAndLabels : objectAndLabelsList) {
      List<String> labels = objectAndLabels.getSecond();
      if(classLabelIndex != null && classLabelIndex >= labels.size()) {
        throw new IllegalArgumentException("No class label at index " + (classLabelIndex) + " specified!");
      }

      String classLabel = null;
      StringBuffer label = new StringBuffer();
      for(int i = 0; i < labels.size(); i++) {
        String l = labels.get(i).trim();
        if(l.length() == 0) {
          continue;
        }

        if(classLabelIndex != null && i == classLabelIndex) {
          classLabel = l;
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

      DatabaseObjectMetadata associationMap = new DatabaseObjectMetadata();
      if(label.length() != 0) {
        associationMap.objectlabel = label.toString();
      }

      if(classLabel != null) {
        try {
          ClassLabel classLabelAssociation = classLabelClass.newInstance();
          classLabelAssociation.init(classLabel);
          associationMap.classlabel = classLabelAssociation;
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

      result.add(new Pair<O, DatabaseObjectMetadata>(objectAndLabels.getFirst(), associationMap));
    }
    return result;
  }

  static class Parameters<O extends DatabaseObject> {
    Database<O> database;

    Integer classLabelIndex;

    Class<? extends ClassLabel> classLabelClass;

    public Parameters(Database<O> database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass) {
      super();
      this.database = database;
      this.classLabelIndex = classLabelIndex;
      this.classLabelClass = classLabelClass;
    }

  }
}