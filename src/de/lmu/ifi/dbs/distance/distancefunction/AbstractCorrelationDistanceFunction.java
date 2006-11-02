package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.DatabaseEvent;
import de.lmu.ifi.dbs.database.DatabaseListener;
import de.lmu.ifi.dbs.distance.CorrelationDistance;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract super class for correlation based distance functions.
 * Provides the Correlation distance for real valued
 * vectors. All subclasses must implement a method to process the preprocessing
 * step in terms of doing the PCA for each object of the database.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractCorrelationDistanceFunction<D extends CorrelationDistance> extends AbstractDistanceFunction<RealVector, D> implements DatabaseListener {
  /**
   * Indicates a separator.
   */
  public static final Pattern SEPARATOR = Pattern.compile("x");

  /**
   * The default preprocessor class name.
   */
  public static String DEFAULT_PREPROCESSOR_CLASS;

  /**
   * Parameter for preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_P = "preprocessor";

  /**
   * The Assocoiation ID for the association to be set by the preprocessor.
   */
  public static AssociationID ASSOCIATION_ID;

  /**
   * Description for parameter preprocessor.
   */
  public static String PREPROCESSOR_CLASS_D;

  /**
   * The super class for the preprocessor.
   */
  public static Class PREPROCESSOR_SUPER_CLASS;

  /**
   * Flag for omission of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_F = "omitPreprocessing";

  /**
   * Description for flag for force of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_D = "flag to omit (a new) preprocessing if for each object a matrix already has been associated.";

  /**
   * True, if preprocessing is omitted, false otherwise.
   */
  private boolean omit;

  /**
   * The preprocessor to run the variance analysis of the objects.
   */
  Preprocessor preprocessor;

  /**
   * Indicates if the verbose flag is set for preprocessing..
   */
  private boolean verbose;

  /**
   * Indicates if the time flag is set for preprocessing.
   */
  boolean time;

  /**
   * Provides a CorrelationDistanceFunction with a pattern defined to accept
   * Strings that define an Integer followed by a separator followed by a
   * Double.
   */
  public AbstractCorrelationDistanceFunction() {
    super(Pattern.compile("\\d+" + AbstractCorrelationDistanceFunction.SEPARATOR.pattern()
                          + "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));

    optionHandler.put(OMIT_PREPROCESSING_F, new Flag(OMIT_PREPROCESSING_F, OMIT_PREPROCESSING_D));

    ClassParameter prepClass = new ClassParameter(PREPROCESSOR_CLASS_P, PREPROCESSOR_CLASS_D, PREPROCESSOR_SUPER_CLASS);
    // TODO default value???
//    prepClass.setDefaultValue(defaultValue);
    optionHandler.put(PREPROCESSOR_CLASS_P, prepClass);
  }

  /**
   * Provides the Correlation distance between the given two vectors.
   *
   * @return the Correlation distance between the given two vectors as an
   *         instance of {@link CorrelationDistance CorrelationDistance}.
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject,
   *      de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public D distance(RealVector rv1, RealVector rv2) {
    return correlationDistance(rv1, rv2);
  }

  /**
   * Returns a description of the class and the required parameters.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("Correlation distance for NumberVectors. Pattern for defining a range: \"" + requiredInputPattern() + "\".", false));
    description.append('\n');
    description.append("Preprocessors available within this framework for distance function ");
    description.append(this.getClass().getName());
    description.append(":\n");
    description.append(Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(PREPROCESSOR_SUPER_CLASS));
    description.append('\n');
    return description.toString();

  }

  /**
   * Computes the necessary PCA associations for each object of the database.
   * Afterwards the database is set to get later on the PCA associations
   * needed for distance computing.
   *
   * @param database the database to be set
   * @param verbose  flag to allow verbose messages while performing the method
   * @param time     flag to request output of performance time
   */
  public void setDatabase(Database<RealVector> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    this.verbose = verbose;
    this.time = time;
    database.addDatabaseListener(this);
    if (!omit || !database.isSet(ASSOCIATION_ID)) {
      preprocessor.run(database, verbose, time);
    }
  }

  /**
   * Sets the values for the parameters delta and preprocessor if specified.
   * If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // preprocessor
    if (optionHandler.isSet(PREPROCESSOR_CLASS_P)) {
      try {
        //noinspection unchecked
        preprocessor = (Preprocessor) Util.instantiate(PREPROCESSOR_SUPER_CLASS, optionHandler.getOptionValue(PREPROCESSOR_CLASS_P));
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(PREPROCESSOR_CLASS_P, optionHandler.getOptionValue(PREPROCESSOR_CLASS_P), PREPROCESSOR_CLASS_D, e);
      }
    }
    else {
      try {
        //noinspection unchecked
        preprocessor = (Preprocessor) Util.instantiate(PREPROCESSOR_SUPER_CLASS, DEFAULT_PREPROCESSOR_CLASS);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(PREPROCESSOR_CLASS_P, optionHandler.getOptionValue(DEFAULT_PREPROCESSOR_CLASS), PREPROCESSOR_CLASS_D, e);
      }
    }

    // omit
    omit = optionHandler.isSet(AbstractCorrelationDistanceFunction.OMIT_PREPROCESSING_F);

    remainingParameters = preprocessor.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(OMIT_PREPROCESSING_F, Boolean.toString(omit));
    attributeSettings.addSetting(PREPROCESSOR_CLASS_P, preprocessor.getClass().getName());

    result.addAll(preprocessor.getAttributeSettings());

    return result;
  }

  /**
   * Invoked after objects of the database have been updated in some way.
   * Use <code>e.getObjects()</code> to get the updated database objects.
   * Runs the preprocessor again.
   */
  public void objectsChanged(DatabaseEvent e) {
    if (!omit) {
      preprocessor.run(getDatabase(), verbose, time);
    }
  }

  /**
   * Invoked after an object has been inserted into the database.
   * Use <code>e.getObjects()</code> to get the newly inserted database objects.
   * Runs the preprocessor again.
   */
  public void objectsInserted(DatabaseEvent e) {
    if (!omit) {
      preprocessor.run(getDatabase(), verbose, time);
    }
  }

  /**
   * Invoked after an object has been deleted from the database.
   * Use <code>e.getObjects()</code> to get the inserted database objects.
   * Runs the preprocessor again.
   */
  public void objectsRemoved(DatabaseEvent e) {
    if (!omit) {
      preprocessor.run(getDatabase(), verbose, time);
    }
  }

  /**
   * Returns the preprocessor of this distance function.
   *
   * @return the preprocessor of this distance function
   */
  public Preprocessor getPreprocessor() {
    return preprocessor;
  }

  /**
   * Computes the correlation distance between the two specified vectors.
   *
   * @param dv1 first RealVector
   * @param dv2 second RealVector
   * @return the correlation distance between the two specified vectors
   */
  abstract D correlationDistance(RealVector dv1, RealVector dv2);
}
