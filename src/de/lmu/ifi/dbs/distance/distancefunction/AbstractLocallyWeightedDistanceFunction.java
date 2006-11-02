package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.DatabaseEvent;
import de.lmu.ifi.dbs.database.DatabaseListener;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * Abstract super class for locally weighted distance functions using a preprocessor
 * to compute the local weight matrix.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractLocallyWeightedDistanceFunction<O extends RealVector> extends AbstractDoubleDistanceFunction<O> implements DatabaseListener {
  /**
   * The default preprocessor class name.
   */
  public static final String DEFAULT_PREPROCESSOR_CLASS = KnnQueryBasedHiCOPreprocessor.class.getName();

  /**
   * Parameter for preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_P = "preprocessor";

  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_D = "the preprocessor to determine the correlation dimensions of the objects " +
                                                    Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Preprocessor.class) +
                                                    ". Default: " + DEFAULT_PREPROCESSOR_CLASS;

  /**
   * Flag for omission of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_F = "omitPreprocessing";

  /**
   * Description for flag for force of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_D = "flag to omit (a new) preprocessing if for each object a matrix already has been associated.";

  /**
   * The Assocoiation ID for the association to be set by the preprocessor.
   */
  public static AssociationID ASSOCIATION_ID;

  /**
   * Whether preprocessing is omitted.
   */
  private boolean omit;

  /**
   * The preprocessor to determine the correlation dimensions of the objects.
   */
  private Preprocessor<O> preprocessor;

  /**
   * Indicates if the verbose flag is set for preprocessing..
   */
  private boolean verbose;

  /**
   * Indicates if the time flag is set for preprocessing.
   */
  private boolean time;

  /**
   * Provides an abstract locally weighted distance function.
   */
  protected AbstractLocallyWeightedDistanceFunction() {
    super();
    // preprocessor
    ClassParameter preprocessorClass = new ClassParameter(PREPROCESSOR_CLASS_P, PREPROCESSOR_CLASS_D, Preprocessor.class);
    preprocessorClass.setDefaultValue(DEFAULT_PREPROCESSOR_CLASS);
    optionHandler.put(PREPROCESSOR_CLASS_P, preprocessorClass);
    // omit preprocessing
    optionHandler.put(OMIT_PREPROCESSING_F, new Flag(OMIT_PREPROCESSING_F, OMIT_PREPROCESSING_D));
  }

  /**
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    this.verbose = verbose;
    this.time = time;
    database.addDatabaseListener(this);

    if (! omit || !database.isSet(ASSOCIATION_ID)) {
      preprocessor.run(getDatabase(), verbose, time);
    }
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // preprocessor
    String preprocessorClass;
    if (optionHandler.isSet(PREPROCESSOR_CLASS_P)) {
      preprocessorClass = optionHandler.getOptionValue(PREPROCESSOR_CLASS_P);
    }
    else {
      preprocessorClass = DEFAULT_PREPROCESSOR_CLASS;
    }
    try {
      //noinspection unchecked
      preprocessor = Util.instantiate(Preprocessor.class, preprocessorClass);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(PREPROCESSOR_CLASS_P, DEFAULT_PREPROCESSOR_CLASS, PREPROCESSOR_CLASS_D, e);
    }

    // omit flag
    omit = optionHandler.isSet(OMIT_PREPROCESSING_F);

    remainingParameters = preprocessor.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings settings = result.get(0);
    settings.addSetting(PREPROCESSOR_CLASS_P, preprocessor.getClass().getName());
    settings.addSetting(OMIT_PREPROCESSING_F, Boolean.toString(omit));

    result.addAll(preprocessor.getAttributeSettings());

    return result;
  }

    /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("Locally weighted distance function. Pattern for defining a range: \"" + requiredInputPattern() + "\".", false));
    description.append('\n');
    description.append("Preprocessors available within this framework for distance function ");
    description.append(this.getClass().getName());
    description.append(":");
    description.append('\n' + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(preprocessor.getClass()));
    description.append('\n');
    return description.toString();
  }

  /**
   * Invoked after objects of the database have been updated in some way.
   * Use <code>e.getObjects()</code> to get the updated database objects.
   */
  public void objectsChanged(DatabaseEvent e) {
    if (! omit) {
      preprocessor.run(getDatabase(), verbose, time);
    }
  }

  /**
   * Invoked after an object has been inserted into the database.
   * Use <code>e.getObjects()</code> to get the newly inserted database objects.
   */
  public void objectsInserted(DatabaseEvent e) {
    if (! omit) {
      preprocessor.run(getDatabase(), verbose, time);
    }
  }

  /**
   * Invoked after an object has been deleted from the database.
   * Use <code>e.getObjects()</code> to get the inserted database objects.
   */
  public void objectsRemoved(DatabaseEvent e) {
    if (! omit) {
      preprocessor.run(getDatabase(), verbose, time);
    }
  }

}
