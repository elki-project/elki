package de.lmu.ifi.dbs.elki;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.application.KDDCLIApplication;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
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
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
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
  private final ObjectListParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ObjectListParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

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
   * Parameter to specify the result evaluator to be used (optional), must
   * extend {@link Evaluator}.
   * <p>
   * Key: {@code -evaluator}
   * </p>
   */
  private final ObjectListParameter<Evaluator<O>> EVALUATOR_PARAM = new ObjectListParameter<Evaluator<O>>(OptionID.EVALUATOR, Evaluator.class, true);

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
  private final ObjectListParameter<ResultHandler<O, Result>> RESULT_HANDLER_PARAM = new ObjectListParameter<ResultHandler<O, Result>>(OptionID.RESULT_HANDLER, ResultHandler.class);

  /**
   * Holds the algorithm to run.
   */
  private List<Algorithm<O, Result>> algorithms;

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
   * Result evaluator.
   */
  private List<Evaluator<O>> evaluators = null;

  /**
   * Output handler.
   */
  private List<ResultHandler<O, Result>> resulthandlers = null;

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
      algorithms = ALGORITHM_PARAM.instantiateClasses(track);
    }

    // parameter database connection
    if(config.grab(DATABASE_CONNECTION_PARAM)) {
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

    if(config.grab(EVALUATOR_PARAM)) {
      evaluators = EVALUATOR_PARAM.instantiateClasses(config);
    }

    settings = track.getAllParameters();

    // result handler - untracked.
    ArrayList<Class<? extends ResultHandler<O, Result>>> defaultHandlers = new ArrayList<Class<? extends ResultHandler<O, Result>>>(1);
    final Class<ResultHandler<O, Result>> rwcls = ClassGenericsUtil.uglyCrossCast(ResultWriter.class, ResultHandler.class);
    defaultHandlers.add(rwcls);
    RESULT_HANDLER_PARAM.setDefaultValue(defaultHandlers);
    if(config.grab(RESULT_HANDLER_PARAM)) {
      resulthandlers = RESULT_HANDLER_PARAM.instantiateClasses(config);
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
    result = new MultiResult();
    for (Algorithm<O, Result> algorithm : algorithms) {
      final Result algResult = algorithm.run(db);
      if (result == null) {
        result = ResultUtil.ensureMultiResult(algResult);
      } else {
        result.addResult(algResult);
      }
    }

    // standard annotations from the source file
    new AnnotationBuiltins(db).prependToResult(result);
    result.prependResult(new IDResult());
    result.prependResult(new SettingsResult(settings));

    // Run evaluation helpers
    if(evaluators != null) {
      for(Evaluator<O> evaluator : evaluators) {
        if(normalizationUndo) {
          evaluator.setNormalization(normalization);
        }
        result = evaluator.processResult(db, result);
      }
    }

    // Run result handlers
    for(ResultHandler<O, Result> resulthandler : resulthandlers) {
      if(normalizationUndo) {
        resulthandler.setNormalization(normalization);
      }
      resulthandler.processResult(db, result);
    }
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