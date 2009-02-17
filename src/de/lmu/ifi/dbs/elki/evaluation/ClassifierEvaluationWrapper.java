package de.lmu.ifi.dbs.elki.evaluation;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.elki.evaluation.holdout.StratifiedCrossValidation;
import de.lmu.ifi.dbs.elki.evaluation.procedure.ClassifierEvaluationProcedure;
import de.lmu.ifi.dbs.elki.evaluation.procedure.EvaluationProcedure;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
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
      "Classname of the classifier algorithm to evaluate. " +
          Properties.ELKI_PROPERTIES.restrictionString(Classifier.class) + "."
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
      "Classname of the evaluation-procedure to use for evaluation " +
          Properties.ELKI_PROPERTIES.restrictionString(EvaluationProcedure.class) + "."
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
   * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.classifier.AbstractClassifier#HOLDOUT_PARAM}
   */
  public static final OptionID HOLDOUT_ID = OptionID.getOrCreateOptionID(
      "classifier.holdout",
      "<Classname of the holdout for evaluation " +
          Properties.ELKI_PROPERTIES.restrictionString(Holdout.class) + "."
  );

  /**
   * Parameter to specify the holdout for evaluation,
   * must extend {@link de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout}.
   * <p>Key: {@code -classifier.holdout} </p>
   * <p>Default value: {@link StratifiedCrossValidation} </p>
   */
  private final ClassParameter<Holdout<O, L>> HOLDOUT_PARAM = new ClassParameter<Holdout<O, L>>(
      HOLDOUT_ID,
      Holdout.class,
      StratifiedCrossValidation.class.getName());

  /**
   * Holds the value of {@link #HOLDOUT_PARAM}.
   */
  protected Holdout<O, L> holdout;

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

      // parameter holdout
      addOption(HOLDOUT_PARAM);
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
      evaluationProcedure.set(database, holdout);

      long starteval = System.currentTimeMillis();
      evaluationResult = evaluationProcedure.evaluate(classifier);
      long endeval = System.currentTimeMillis();
      if (this.isTime()) {
          verbose("time required for evaluation: " + (endeval - starteval) + " msec.");
      }
      return evaluationResult;
  }

  public final EvaluationResult<O,Classifier<O,L,Result>> getResult() {
      return evaluationResult;
  }
  
  /**
   * Calls the super method
   * and instantiates {@link #evaluationProcedure} according to the value of parameter {@link #EVALUATION_PROCEDURE_PARAM}
   * and {@link #holdout} according to the value of parameter {@link #HOLDOUT_PARAM}.
   * The remaining parameters are passed to the {@link #evaluationProcedure}
   * and, then, to the {@link #holdout}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
      String[] remainingParameters = super.setParameters(args);

      // parameter evaluation procedure
      classifier = CLASSIFIER_ALGORITHM_PARAM.instantiateClass();
      classifier.setTime(isTime());
      classifier.setVerbose(isVerbose());
      remainingParameters = classifier.setParameters(remainingParameters);

      // parameter evaluation procedure
      evaluationProcedure = EVALUATION_PROCEDURE_PARAM.instantiateClass();
      evaluationProcedure.setTime(isTime());
      evaluationProcedure.setVerbose(isVerbose());
      remainingParameters = evaluationProcedure.setParameters(remainingParameters);

      // parameter holdout
      holdout = HOLDOUT_PARAM.instantiateClass();
      remainingParameters = holdout.setParameters(remainingParameters);

      setParameters(args, remainingParameters);
      return remainingParameters;
  }

  /**
   * Calls the super method
   * and adds to the returned attribute settings the attribute settings of
   * {@link #evaluationProcedure} and {@link #holdout}.
   *
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
      List<AttributeSettings> attributeSettings = super.getAttributeSettings();

      attributeSettings.addAll(classifier.getAttributeSettings());
      attributeSettings.addAll(evaluationProcedure.getAttributeSettings());
      attributeSettings.addAll(holdout.getAttributeSettings());

      return attributeSettings;
  }

  /**
   * Calls the super method
   * and appends the parameter description of
   * {@link #evaluationProcedure} and {@link #holdout} if they are already initialized.
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
      // holdout
      if (holdout != null) {
          description.append(holdout.parameterDescription());
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
