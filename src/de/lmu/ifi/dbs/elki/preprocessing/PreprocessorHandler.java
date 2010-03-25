package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Handler class for all objects (e.g. distance functions) using a preprocessor
 * running on a certain database.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to run the Preprocessor on
 * @param <P> the type of Preprocessor used
 */
// FIXME: Re-add "timing" option!
public class PreprocessorHandler<O extends DatabaseObject, P extends Preprocessor<O>> extends AbstractLoggable implements DatabaseListener {
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
  private final ObjectParameter<P> PREPROCESSOR_PARAM;

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
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   * @param preprocessorClient the class using this handler
   */
  public PreprocessorHandler(Parameterization config, PreprocessorClient<P,O> preprocessorClient) {
    // preprocessor
    Class<P> pcls = preprocessorClient.getPreprocessorSuperClass();
    PREPROCESSOR_PARAM = new ObjectParameter<P>(PREPROCESSOR_ID, pcls, preprocessorClient.getDefaultPreprocessorClass());
    PREPROCESSOR_PARAM.setShortDescription(preprocessorClient.getPreprocessorDescription());
    if (config.grab(PREPROCESSOR_PARAM) && PREPROCESSOR_PARAM.getValue() != null) {
      preprocessor = PREPROCESSOR_PARAM.instantiateClass(config);
    }

    // omit flag
    if (config.grab(OMIT_PREPROCESSING_FLAG)) {
      omit = OMIT_PREPROCESSING_FLAG.getValue();
    }

    // association id
    this.associationID = preprocessorClient.getAssociationID();
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
   */
  public void runPreprocessor(Database<O> database) {
    this.database = database;
    if(!omit || !((database.isSet(associationID) || database.isSetGlobally(associationID)))) {
      preprocessor.run(database, verbose, time);
    }
    database.addDatabaseListener(this);
  }
}