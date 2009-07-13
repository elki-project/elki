package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Handler class for all objects (e.g. distance functions) using a preprocessor
 * running on a certain database.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to run the Preprocessor on
 * @param <P> the type of Preprocessor used
 */
public class PreprocessorHandler<O extends DatabaseObject, P extends Preprocessor<O>> extends AbstractParameterizable implements DatabaseListener {

  /**
   * OptionID for {@link #PREPROCESSOR_PARAM}
   */
  public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID("preprocessorhandler.preprocessor", "Classname of the preprocessor to be used.");

  /**
   * Parameter to specify the preprocessor to be used, must extend at least
   * {@link Preprocessor}.
   * <p>
   * Key: {@code -distancefunction.preprocessor}
   * </p>
   */
  private final ClassParameter<P> PREPROCESSOR_PARAM;

  /**
   * Holds the instance of the preprocessor specified by
   * {@link #PREPROCESSOR_PARAM}.
   */
  private P preprocessor;

  /**
   * OptionID for {@link #OMIT_PREPROCESSING_FLAG}
   */
  public static final OptionID OMIT_PREPROCESSING_ID = OptionID.getOrCreateOptionID("preprocessorhandler.omitPreprocessing", "Flag to omit (a new) preprocessing if for each object the association has already been set.");

  /**
   * Flag to omit (a new) preprocessing if for each object the association has
   * already been set.
   * <p>
   * Key: {@code -distancefunction.omitPreprocessing}
   * </p>
   */
  private final Flag OMIT_PREPROCESSING_FLAG = new Flag(OMIT_PREPROCESSING_ID);

  /**
   * Holds the value of {@link #OMIT_PREPROCESSING_FLAG}.
   */
  private boolean omit;

  /**
   * The association ID for the association to be set by the preprocessor.
   */
  private AssociationID<?> associationID;

  /**
   * Indicates if the verbose flag is set for preprocessing.
   */
  private boolean verbose;

  /**
   * Indicates if the time flag is set for preprocessing.
   */
  private boolean time;

  /**
   * The database to run the preprocessor on.
   */
  private Database<O> database;

  /**
   * Provides a handler class for all objects using a preprocessor, adding
   * parameter {@link #PREPROCESSOR_PARAM} and flag
   * {@link #OMIT_PREPROCESSING_FLAG} to the option handler additionally to
   * parameters of super class.
   * 
   * @param preprocessorClient the class using this handler
   */
  public PreprocessorHandler(PreprocessorClient<P,O> preprocessorClient) {

    // preprocessor
    Class<P> pcls = preprocessorClient.getPreprocessorSuperClass();
    PREPROCESSOR_PARAM = new ClassParameter<P>(PREPROCESSOR_ID, pcls, preprocessorClient.getDefaultPreprocessorClassName());
    PREPROCESSOR_PARAM.setShortDescription(preprocessorClient.getPreprocessorDescription());
    addOption(PREPROCESSOR_PARAM);

    // omit flag
    addOption(OMIT_PREPROCESSING_FLAG);

    // association id
    this.associationID = preprocessorClient.getAssociationID();
  }

  /**
   * Calls
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters
   * AbstractParameterizable#setParameters} and sets additionally the
   * value of flag {@link #OMIT_PREPROCESSING_FLAG} and instantiates
   * {@link #preprocessor} according to the value of parameter
   * {@link #PREPROCESSOR_PARAM} The remaining parameters are passed to the
   * {@link #preprocessor}.
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // omit
    omit = OMIT_PREPROCESSING_FLAG.isSet();

    // preprocessor
    preprocessor = PREPROCESSOR_PARAM.instantiateClass();
    addParameterizable(preprocessor);
    remainingParameters = preprocessor.setParameters(remainingParameters);

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Invoked after objects of the database have been updated in some way. Runs
   * the preprocessor if {@link #OMIT_PREPROCESSING_FLAG} is not set.
   * 
   * @param e unused
   */
  public void objectsChanged(DatabaseEvent e) {
    if(!omit) {
      preprocessor.run(database, verbose, time);
    }
  }

  /**
   * Invoked after an object has been inserted into the database. Runs the
   * preprocessor if {@link #OMIT_PREPROCESSING_FLAG} is not set.
   * 
   * @param e unused
   */
  public void objectsInserted(DatabaseEvent e) {
    if(!omit) {
      preprocessor.run(database, verbose, time);
    }
  }

  /**
   * Invoked after an object has been deleted from the database. Runs the
   * preprocessor if {@link #OMIT_PREPROCESSING_FLAG} is not set.
   * 
   * @param e unused
   */
  public void objectsRemoved(DatabaseEvent e) {
    if(!omit) {
      preprocessor.run(database, verbose, time);
    }
  }

  /**
   * Returns the preprocessor.
   * 
   * @return the instance of the preprocessor specified by
   *         {@link #PREPROCESSOR_PARAM}
   */
  public P getPreprocessor() {
    return preprocessor;
  }

  /**
   * Runs the preprocessor on the database if the omit flag is not set or the
   * database does contain the association id neither for any id nor as global
   * association.
   * 
   * @param database the database to run the preprocessor on
   * @param verbose flag to allow verbose messages while performing the method
   * @param time flag to request output of performance time
   */
  public void runPreprocessor(Database<O> database, boolean verbose, boolean time) {
    this.database = database;
    this.verbose = verbose;
    this.time = time;

    if(!omit || !((database.isSet(associationID) || database.isSetGlobally(associationID)))) {
      preprocessor.run(database, verbose, time);
    }
    database.addDatabaseListener(this);
  }
}
