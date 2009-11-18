package de.lmu.ifi.dbs.elki;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection
 * DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class KDDTask<O extends DatabaseObject> extends AbstractApplication {
  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ClassParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ClassParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

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
  private final ClassParameter<DatabaseConnection<O>> DATABASE_CONNECTION_PARAM = new ClassParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class.getName());

  /**
   * Optional Parameter to specify a normalization in order to use a database
   * with normalized values.
   * <p>
   * Key: {@code -norm}
   * </p>
   */
  private final ClassParameter<Normalization<O>> NORMALIZATION_PARAM = new ClassParameter<Normalization<O>>(OptionID.NORMALIZATION, Normalization.class, true);

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
  private final ClassParameter<ResultHandler<O, Result>> RESULT_HANDLER_PARAM = new ClassParameter<ResultHandler<O, Result>>(OptionID.RESULT_HANDLER, ResultHandler.class, ResultWriter.class.getName());

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
   * Provides a KDDTask.
   */
  public KDDTask() {
    super();

    // parameter algorithm
    addOption(ALGORITHM_PARAM);

    // parameter database connection
    addOption(DATABASE_CONNECTION_PARAM);

    // result handler
    addOption(RESULT_HANDLER_PARAM);

    // parameter normalization
    addOption(NORMALIZATION_PARAM);

    // normalization-undo flag
    addOption(NORMALIZATION_UNDO_FLAG);

    // normalization-undo depends on a defined normalization.
    GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<String, String>(NORMALIZATION_PARAM, null, NORMALIZATION_UNDO_FLAG, true);
    optionHandler.setGlobalParameterConstraint(gpc);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // algorithm
    algorithm = ALGORITHM_PARAM.instantiateClass();
    addParameterizable(algorithm);
    remainingParameters = algorithm.setParameters(remainingParameters);

    // database connection
    databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass();
    addParameterizable(databaseConnection);
    remainingParameters = databaseConnection.setParameters(remainingParameters);

    // result handler
    resulthandler = RESULT_HANDLER_PARAM.instantiateClass();
    addParameterizable(resulthandler);
    remainingParameters = resulthandler.setParameters(remainingParameters);

    // normalization
    if(NORMALIZATION_PARAM.isSet()) {
      normalization = NORMALIZATION_PARAM.instantiateClass();
      normalizationUndo = NORMALIZATION_UNDO_FLAG.isSet();
      addParameterizable(normalization);
      remainingParameters = normalization.setParameters(remainingParameters);
    }

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Method to run the specified algorithm using the specified database
   * connection.
   */
  @Override
  public void run() throws IllegalStateException {
    Database<O> db = databaseConnection.getDatabase(normalization);
    algorithm.run(db);
    result = ResultUtil.ensureMultiResult(algorithm.getResult());

    // standard annotations from the source file
    // TODO: get them via databaseConnection!
    // adding them here will make the output writer think
    // that they were an part of the actual result.
    AnnotationFromDatabase<String, O> ar1 = new AnnotationFromDatabase<String, O>(db, AssociationID.LABEL);
    AnnotationFromDatabase<ClassLabel, O> ar2 = new AnnotationFromDatabase<ClassLabel, O>(db, AssociationID.CLASS);

    result.prependResult(ar1);
    result.prependResult(ar2);

    List<AttributeSettings> settings = getAttributeSettings();
    ResultUtil.setGlobalAssociation(result, AssociationID.META_SETTINGS, settings);

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
    (new KDDTask<DatabaseObject>()).runCLIApplication(args);
  }
}