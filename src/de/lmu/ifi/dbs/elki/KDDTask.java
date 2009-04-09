package de.lmu.ifi.dbs.elki;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.AnnotationsFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection
 * DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class KDDTask<O extends DatabaseObject> extends AbstractParameterizable {

  /**
   * Information for citation and version.
   */
  public static final String INFORMATION = "ELKI Version 0.1 (2008, July)\n\n" + "published in:\n" + "Elke Achtert, Hans-Peter Kriegel, Arthur Zimek:\n" + "ELKI: A Software System for Evaluation of Subspace Clustering Algorithms.\n" + "In Proc. 20th International Conference on Scientific and Statistical Database Management (SSDBM 2008), Hong Kong, China, 2008.";

  /**
   * The String for calling this class' main routine on command line interface.
   */
  private static final String CALL = "java " + KDDTask.class.getName();

  /**
   * The newline string according to system.
   */
  private static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Flag to obtain help-message.
   * <p>
   * Key: {@code -h}
   * </p>
   */
  private final Flag HELP_FLAG = new Flag(OptionID.HELP);

  /**
   * Flag to obtain help-message.
   * <p>
   * Key: {@code -help}
   * </p>
   */
  private final Flag HELP_LONG_FLAG = new Flag(OptionID.HELP_LONG);

  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ClassParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ClassParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * Optional Parameter to specify a class to obtain a description for, must
   * extend {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * .
   * <p>
   * Key: {@code -description}
   * </p>
   */
  private final ClassParameter<Parameterizable> DESCRIPTION_PARAM = new ClassParameter<Parameterizable>(OptionID.DESCRIPTION, Parameterizable.class, true);

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
   * Parameter to specify the database connection to be used, must extend
   * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection}.
   * <p>
   * Key: {@code -dbc}
   * </p>
   * <p>
   * Default value: {@link FileBasedDatabaseConnection}
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
   * Whether KDDTask has been properly initialized for calling the
   * {@link #run() run()}-method.
   */
  private boolean initialized = false;

  /**
   * A normalization - per default no normalization is used.
   */
  private Normalization<O> normalization = null;

  /**
   * Output handler.
   */
  private ResultHandler<O, Result> resulthandler = null;

  /**
   * Whether to undo normalization for result.
   */
  private boolean normalizationUndo = false;

  private OptionHandler helpOptionHandler;

  /**
   * Provides a KDDTask.
   */
  public KDDTask() {

    helpOptionHandler = new OptionHandler(new TreeMap<String, Option<?>>(), this.getClass().getName());
    helpOptionHandler.put(HELP_FLAG);
    helpOptionHandler.put(HELP_LONG_FLAG);
    helpOptionHandler.put(DESCRIPTION_PARAM);

    // parameter algorithm
    addOption(ALGORITHM_PARAM);

    // help flag
    addOption(HELP_FLAG);
    addOption(HELP_LONG_FLAG);

    // description parameter
    addOption(DESCRIPTION_PARAM);

    // parameter database connection
    addOption(DATABASE_CONNECTION_PARAM);
    
    // result handler
    addOption(RESULT_HANDLER_PARAM);

    // parameter normalization
    addOption(NORMALIZATION_PARAM);

    // normalization-undo flag
    addOption(NORMALIZATION_UNDO_FLAG);

    optionHandler.setProgrammCall(CALL);
  }

  /**
   * Returns a description for printing on command line interface.
   */
  @Override
  public String parameterDescription() {
    return optionHandler.usage("", true);
  }

  /**
   * Returns a usage message with the specified message as leading line, and
   * information as provided by optionHandler. If an algorithm is specified, the
   * description of the algorithm is returned.
   * 
   * @param message a message to be include in the usage message
   * @return a usage message with the specified message as leading line, and
   *         information as provided by optionHandler
   */
  public String usage(String message) {
    StringBuffer usage = new StringBuffer();
    usage.append(message);
    usage.append(NEWLINE);
    usage.append(optionHandler.usage("", false));
    usage.append(NEWLINE);
    if(algorithm != null) {
      usage.append(OptionHandler.OPTION_PREFIX);
      usage.append(ALGORITHM_PARAM.getName());
      usage.append(" ");
      usage.append(algorithm.parameterDescription());
      usage.append(NEWLINE);
    }
    if(resulthandler != null) {
      usage.append(OptionHandler.OPTION_PREFIX);
      usage.append(RESULT_HANDLER_PARAM.getName());
      usage.append(" ");
      usage.append(resulthandler.parameterDescription());
      usage.append(NEWLINE);
    }
    return usage.toString();
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    if(args.length == 0) {
      throw new AbortException("No options specified. Try flag -h to gain more information.");
    }
    helpOptionHandler.grabOptions(args);

    // description
    if(helpOptionHandler.isSet(DESCRIPTION_PARAM)) {
      String descriptionClass = DESCRIPTION_PARAM.getValue();
      Parameterizable p;
      try {
        try {
          p = ClassGenericsUtil.instantiate(Algorithm.class, descriptionClass);
        }
        catch(UnableToComplyException e) {
          p = ClassGenericsUtil.instantiate(Parameterizable.class, descriptionClass);
        }
      }
      catch(UnableToComplyException e) {
        // FIXME: log here?
        LoggingUtil.exception(e.getMessage(), e);
        throw new WrongParameterValueException(DESCRIPTION_PARAM.getName(), descriptionClass, DESCRIPTION_PARAM.getDescription(), e);
      }
      if(p instanceof Algorithm) {
        Algorithm<?, ?> a = (Algorithm<?, ?>) p;
        throw new AbortException(a.getDescription().toString() + '\n' + a.parameterDescription());
      }
      else {
        throw new AbortException(p.parameterDescription());
      }
    }
    
    String[] remainingParameters = super.setParameters(args);

    // algorithm
    algorithm = ALGORITHM_PARAM.instantiateClass();
    remainingParameters = algorithm.setParameters(remainingParameters);

    // database connection
    databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass();
    remainingParameters = databaseConnection.setParameters(remainingParameters);

    // result handler
    resulthandler = RESULT_HANDLER_PARAM.instantiateClass();
    remainingParameters = resulthandler.setParameters(remainingParameters);

    // normalization
    if(NORMALIZATION_PARAM.isSet()) {
      normalization = NORMALIZATION_PARAM.instantiateClass();
      normalizationUndo = NORMALIZATION_UNDO_FLAG.isSet();
      remainingParameters = normalization.setParameters(remainingParameters);
    }
    else if(NORMALIZATION_UNDO_FLAG.isSet()) {
      throw new WrongParameterValueException("Illegal parameter setting: Flag " + NORMALIZATION_UNDO_FLAG + " is set, but no normalization is specified.");
    }

    // help
    if(helpOptionHandler.isSet(HELP_FLAG) || helpOptionHandler.isSet(HELP_LONG_FLAG)) {
      throw new AbortException(INFORMATION);
    }
    
    initialized = true;
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Calls the super method and adds to the returned attribute settings the
   * attribute settings of the {@link #databaseConnection}, the
   * {@link #normalization}, and {@link #algorithm}.
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    attributeSettings.addAll(databaseConnection.getAttributeSettings());
    if(normalization != null) {
      attributeSettings.addAll(normalization.getAttributeSettings());
    }
    attributeSettings.addAll(algorithm.getAttributeSettings());

    return attributeSettings;
  }

  /**
   * Method to run the specified algorithm using the specified database
   * connection.
   * 
   * @return the result of the specified algorithm
   * @throws IllegalStateException if initialization has not been done properly
   *         (i.e. {@link #setParameters(String[]) setParameters(String[])} has
   *         not been called before calling this method)
   */
  public MultiResult run() throws IllegalStateException {
    if(initialized) {
      Database<O> db = databaseConnection.getDatabase(normalization);
      algorithm.run(db);
      MultiResult result;
      Result res = algorithm.getResult();

      // standard annotations from the source file
      // TODO: get them via databaseConnection!
      // adding them here will make the output writer think
      // that they were an part of the actual result.
      AnnotationsFromDatabase<O, ?> ar = new AnnotationsFromDatabase<O, Object>(db);
      ar.addAssociationGenerics(null, AssociationID.LABEL);
      ar.addAssociationGenerics(null, AssociationID.CLASS);

      // insert standard annotations when we have a MultiResult
      if(res instanceof MultiResult) {
        result = (MultiResult) res;
        result.prependResult(ar);
      }
      else {
        // TODO: can we always wrap them in a MultiResult safely?
        result = new MultiResult();
        result.addResult(ar);
        result.addResult(res);
      }

      if(result != null) {
        List<AttributeSettings> settings = getAttributeSettings();
        if(normalizationUndo) {
          resulthandler.setNormalization(normalization);
        }
        resulthandler.processResult(db, result, settings);
      }
      return result;
    }
    else {
      throw new IllegalStateException(KDDTask.class.getName() + " was not properly initialized. Need to set parameters first.");
    }
  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    LoggingConfiguration.assertConfigured();
    Logging logger = Logging.getLogger(KDDTask.class);
    KDDTask<? extends DatabaseObject> kddTask = new KDDTask<DatabaseObject>();
    try {
      String[] remainingParameters = kddTask.setParameters(args);
      if(remainingParameters.length != 0) {
        logger.warning("Unnecessary parameters specified: " + Arrays.asList(remainingParameters) + "\n");
      }
      kddTask.run();
    }
    catch(AbortException e) {
      // ensure we actually show the message:
      LoggingConfiguration.setVerbose(true);
      logger.verbose(kddTask.usage(e.getMessage() + "\n\nUSAGE:"));
    }
    catch(ParameterException e) {
      logger.warning(e.getMessage(), e);
    }
    // any other exception
    catch(Exception e) {
      LoggingUtil.exception(e.getMessage(), e);
    }
  }
}