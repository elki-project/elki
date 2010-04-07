package de.lmu.ifi.dbs.elki;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.application.KDDCLIApplication;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.AnnotationBuiltins;
import de.lmu.ifi.dbs.elki.result.IDResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection
 * DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class KDDTask<O extends DatabaseObject> extends AbstractLoggable implements Parameterizable {
  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  // TODO: ObjectListParameter, once the UI supports this.
  private final ObjectParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ObjectParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * Parameter to specify the database connection to be used, must extend
   * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection}.
   * <p>
   * Key: {@code -dbc}
   * </p>
   * <p>
   * Default value: {@link FileBasedDatabaseConnection}
   * </p>
   */
  private final ObjectParameter<DatabaseConnection<O>> DATABASE_CONNECTION_PARAM = new ObjectParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);

  /**
   * Optional Parameter to specify a normalization in order to use a database
   * with normalized values.
   * <p>
   * Key: {@code -norm}
   * </p>
   */
  private final ObjectParameter<Normalization<O>> NORMALIZATION_PARAM = new ObjectParameter<Normalization<O>>(OptionID.NORMALIZATION, Normalization.class, true);

  /**
   * Flag to revert result to original values - invalid option if no
   * normalization has been performed.
   * <p>
   * Key: {@code -normUndo}
   * </p>
   */
  private final Flag NORMALIZATION_UNDO_FLAG = new Flag(OptionID.NORMALIZATION_UNDO);

  /**
   * Parameter to specify the result handler to be used, must extend
   * {@link ResultHandler}.
   * <p>
   * Key: {@code -resulthandler}
   * </p>
   * <p>
   * Default value: {@link ResultWriter}
   * </p>
   */
  // TODO: ObjectListParameter, once the UI supports this.
  private final ObjectParameter<ResultHandler<O, Result>> RESULT_HANDLER_PARAM = new ObjectParameter<ResultHandler<O, Result>>(OptionID.RESULT_HANDLER, ResultHandler.class, ResultWriter.class);

  /**
   * Holds the algorithm to run.
   */
  private Algorithm<O, Result> algorithm;

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection<O> databaseConnection;

  /**
   * A normalization - per default no normalization is used.
   */
  private Normalization<O> normalization = null;

  /**
   * Whether to undo normalization for result.
   */
  private boolean normalizationUndo = false;

  /**
   * Output handler.
   */
  private ResultHandler<O, Result> resulthandler = null;

  /**
   * Store the result.
   */
  MultiResult result = null;

  /**
   * The settings used, for settings reporting.
   */
  private Collection<Pair<Object, Parameter<?, ?>>> settings;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public KDDTask(Parameterization config) {
    super();
    
    TrackParameters track = new TrackParameters(config);

    // parameter algorithm
    if(config.grab(ALGORITHM_PARAM)) {
      algorithm = ALGORITHM_PARAM.instantiateClass(track);
    }

    // parameter database connection
    if (config.grab(DATABASE_CONNECTION_PARAM)) {
      databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass(track);
    }

    // parameter normalization
    config.grab(NORMALIZATION_PARAM);
    config.grab(NORMALIZATION_UNDO_FLAG);
    // normalization-undo depends on a defined normalization.
    GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Class<?>, Class<? extends Normalization<O>>>(NORMALIZATION_PARAM, null, NORMALIZATION_UNDO_FLAG, true);
    config.checkConstraint(gpc);
    if(NORMALIZATION_PARAM.isDefined()) {
      normalization = NORMALIZATION_PARAM.instantiateClass(track);
      normalizationUndo = NORMALIZATION_UNDO_FLAG.getValue();
    }
    
    settings = track.getAllParameters();

    // result handler - untracked.
    if (config.grab(RESULT_HANDLER_PARAM)) {
      resulthandler = RESULT_HANDLER_PARAM.instantiateClass(config);
    }
  }

  /**
   * Method to run the specified algorithm using the specified database
   * connection.
   * 
   * @throws IllegalStateException on execution errors 
   */
  public void run() throws IllegalStateException {
    Database<O> db = databaseConnection.getDatabase(normalization);
    result = ResultUtil.ensureMultiResult(algorithm.run(db));

    // standard annotations from the source file
    new AnnotationBuiltins(db).prependToResult(result);
    result.prependResult(new IDResult());
    result.prependResult(new SettingsResult(settings));

    if(normalizationUndo) {
      resulthandler.setNormalization(normalization);
    }
    resulthandler.processResult(db, result);
  }

  /**
   * Get the algorithms result.
   * 
   * @return the result
   */
  public MultiResult getResult() {
    return result;
  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    KDDCLIApplication.runCLIApplication(KDDCLIApplication.class, args);
  }
}