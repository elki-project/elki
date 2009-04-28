package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.procedure.ClassifierEvaluationProcedure;
import de.lmu.ifi.dbs.elki.evaluation.procedure.EvaluationProcedure;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Wrapper to run an evaluation procedure on a classifier algorithm.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <L> Class label type
 */
public class ClassifierEvaluationWrapper<O extends DatabaseObject, L extends ClassLabel> extends AbstractAlgorithm<O, EvaluationResult<O,Classifier<O,L,Result>>> {
  /**
   * OptionID for {@link #CLASSIFIER_ALGORITHM_PARAM}
   */
  public static final OptionID CLASSIFIER_ALGORITHM_ID = OptionID.getOrCreateOptionID(
      "classifier.algorithm",
      "Classifier algorithm to evaluate."
  );

  /**
   * Parameter to specify the evaluation-procedure to use for evaluation,
   * must extend {@link EvaluationProcedure}.
   * <p>Key: {@code -classifier.eval} </p>
   */
  private final ClassParameter<Classifier<O, L, Result>> CLASSIFIER_ALGORITHM_PARAM =
      new ClassParameter<Classifier<O, L, Result>>(CLASSIFIER_ALGORITHM_ID,
          Classifier.class);

  /**
   * Classifier algorithm to use.
   */
  protected Classifier<O, L, Result> classifier;

  /**
   * OptionID for {@link #EVALUATION_PROCEDURE_PARAM}
   */
  public static final OptionID EVALUATION_PROCEDURE_ID = OptionID.getOrCreateOptionID(
      "classifier.eval",
      "Evaluation-procedure to use."
  );

  /**
   * Parameter to specify the evaluation-procedure to use for evaluation,
   * must extend {@link EvaluationProcedure}.
   * <p>Key: {@code -classifier.eval} </p>
   * <p>Default value: {@link ClassifierEvaluationProcedure} </p>
   */
  private final ClassParameter<EvaluationProcedure<O, L, Classifier<O, L, Result>>> EVALUATION_PROCEDURE_PARAM =
      new ClassParameter<EvaluationProcedure<O, L, Classifier<O, L, Result>>>(EVALUATION_PROCEDURE_ID,
          EvaluationProcedure.class, ClassifierEvaluationProcedure.class.getName());
  
  /**
   * Holds the value of {@link #EVALUATION_PROCEDURE_PARAM}.
   */
  protected EvaluationProcedure<O, L, Classifier<O, L, Result>> evaluationProcedure;

  /**
   * The result.
   */
  private EvaluationResult<O, Classifier<O, L, Result>> evaluationResult;

  /**
   * Adds parameters {@link #EVALUATION_PROCEDURE_PARAM} and {@link #HOLDOUT_PARAM}
   * to the option handler additionally to parameters of super class.
   */
  protected ClassifierEvaluationWrapper() {
      super();
      
      // classification algorithm
      addOption(CLASSIFIER_ALGORITHM_PARAM);

      // parameter evaluation procedure
      addOption(EVALUATION_PROCEDURE_PARAM);
  }
  
  /**
   * Evaluates this algorithm on the given database using the currently set
   * evaluation procedure and holdout. The result of the evaluation procedure
   * is provided as result of this algorithm. The time for the complete
   * evaluation is given if the flag time is set. Whether to assess time and
   * give verbose comments in single evaluation steps is passed to the
   * evaluation procedure matching the setting of the flags time and verbose.
   *
   * @param database the database to build the model on
   * @throws IllegalStateException if the classifier is not properly initiated (e.g. parameters
   *                               are not set)
   */
  @Override
  protected final EvaluationResult<O,Classifier<O,L,Result>> runInTime(Database<O> database) throws IllegalStateException {
      evaluationProcedure.setTime(this.isTime());
      evaluationProcedure.setVerbose(this.isVerbose());

      long starteval = System.currentTimeMillis();
      evaluationResult = evaluationProcedure.evaluate(database, classifier);
      long endeval = System.currentTimeMillis();
      if (this.isTime()) {
        logger.verbose("time required for evaluation: " + (endeval - starteval) + " msec.");
      }
      return evaluationResult;
  }

  public final EvaluationResult<O,Classifier<O,L,Result>> getResult() {
      return evaluationResult;
  }
  
  /**
   * Calls the super method
   * and instantiates {@link #evaluationProcedure} according to the value of parameter {@link #EVALUATION_PROCEDURE_PARAM}.
   * The remaining parameters are passed to the {@link #evaluationProcedure}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
      String[] remainingParameters = super.setParameters(args);

      // parameter evaluation procedure
      classifier = CLASSIFIER_ALGORITHM_PARAM.instantiateClass();
      classifier.setTime(isTime());
      classifier.setVerbose(isVerbose());
      remainingParameters = classifier.setParameters(remainingParameters);
      addParameterizable(classifier);

      // parameter evaluation procedure
      evaluationProcedure = EVALUATION_PROCEDURE_PARAM.instantiateClass();
      evaluationProcedure.setTime(isTime());
      evaluationProcedure.setVerbose(isVerbose());
      remainingParameters = evaluationProcedure.setParameters(remainingParameters);
      addParameterizable(evaluationProcedure);

      rememberParametersExcept(args, remainingParameters);
      return remainingParameters;
  }

  /**
   * Calls the super method
   * and appends the parameter description of
   * {@link #evaluationProcedure}  if they are already initialized.
   */
  @Override
  public String parameterDescription() {
      StringBuilder description = new StringBuilder();
      description.append(super.parameterDescription());
      // classification procedure
      if (classifier != null) {
          description.append(classifier.parameterDescription());
      }

          // evaluationProcedure
      if (evaluationProcedure != null) {
          description.append(evaluationProcedure.parameterDescription());
      }

      return description.toString();
  }

  /**
   * Algorithm description.
   */
  @Override
  public Description getDescription() {
    return new Description("ClassifierEvaluationWrapper", "Wrapper for evaluating a classifier.","Wrapper to run an evaluation procedure on a classifier algorithm.","");
  }
}
