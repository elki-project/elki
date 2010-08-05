package de.lmu.ifi.dbs.elki;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.application.KDDCLIApplication;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationBuiltins;
import de.lmu.ifi.dbs.elki.result.IDResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;
import de.lmu.ifi.dbs.elki.workflow.EvaluationStep;
import de.lmu.ifi.dbs.elki.workflow.InputStep;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection
 * DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class KDDTask<O extends DatabaseObject> implements Parameterizable {
  /**
   * The settings used, for settings reporting.
   */
  private Collection<Pair<Object, Parameter<?, ?>>> settings;

  /**
   * The data input step
   */
  private InputStep<O> inputStep;

  /**
   * The algorithm (data mining) step.
   */
  private AlgorithmStep<O> algorithmStep;

  /**
   * The evaluation step.
   */
  private EvaluationStep<O> evaluationStep;

  /**
   * The output/visualization step
   */
  private OutputStep<O> outputStep;

  /**
   * The result object.
   */
  private MultiResult result;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public KDDTask(Parameterization config) {
    super();
    config = config.descend(this);
    TrackParameters track = new TrackParameters(config);

    inputStep = new InputStep<O>(track);
    algorithmStep = new AlgorithmStep<O>(track);
    evaluationStep = new EvaluationStep<O>(track);

    // We don't include output parameters
    settings = track.getAllParameters();
    // configure output with the original parameterization
    outputStep = new OutputStep<O>(config);
  }

  /**
   * Method to run the specified algorithm using the specified database
   * connection.
   * 
   * @throws IllegalStateException on execution errors
   */
  public void run() throws IllegalStateException {
    // Input step
    Database<O> db = inputStep.getDatabase();

    // Algorithms - Data Mining Step
    result = algorithmStep.runAlgorithms(db);

    // standard annotations from the source file
    // TODO: handle this differently...
    new AnnotationBuiltins(db).prependToResult(result);
    result.prependResult(new IDResult());
    result.prependResult(new SettingsResult(settings));

    // Evaluation
    result = evaluationStep.runEvaluators(result, db, inputStep.getNormalizationUndo(), inputStep.getNormalization());

    // Output / Visualization
    outputStep.runResultHandlers(result, db, inputStep.getNormalizationUndo(), inputStep.getNormalization());
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