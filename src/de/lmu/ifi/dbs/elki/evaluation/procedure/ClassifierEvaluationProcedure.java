package de.lmu.ifi.dbs.elki.evaluation.procedure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedSet;

import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.ConfusionMatrix;
import de.lmu.ifi.dbs.elki.evaluation.ConfusionMatrixBasedEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.EvaluationResult;
import de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.elki.evaluation.holdout.StratifiedCrossValidation;
import de.lmu.ifi.dbs.elki.evaluation.holdout.TrainingAndTestSet;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Class to evaluate a classifier using a specified holdout or a provided pair
 * of training and test data.
 * 
 * @author Arthur Zimek
 */
// TODO: add complete support for test&training pair.
public class ClassifierEvaluationProcedure<O extends DatabaseObject, L extends ClassLabel, C extends Classifier<O, L, Result>> extends AbstractParameterizable implements EvaluationProcedure<O, L, C> {
  /**
   * Flag to request verbose information.
   */
  private final Flag TIME_FLAG = new Flag(OptionID.ALGORITHM_TIME);

  /**
   * Flag to request verbose information.
   */
  private final Flag VERBOSE_FLAG = new Flag(OptionID.ALGORITHM_VERBOSE);

  /**
   * OptionID for {@link #HOLDOUT_PARAM}
   */
  public static final OptionID HOLDOUT_ID = OptionID.getOrCreateOptionID(
      "classifier.holdout",
      "Holdout class used in evaluation."
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
   * Holds whether to assess runtime during the evaluation.
   */
  protected boolean time = false;

  /**
   * Holds whether to print verbose messages during evaluation.
   */
  protected boolean verbose = false;

  /**
   * Holds the class labels.
   */
  protected L[] labels;

  /**
   * Holds the holdout.
   */
  private Holdout<O, L> holdout;

  /**
   * Holds the partitions.
   */
  private TrainingAndTestSet<O, L>[] partition;

  /**
   * Provides a ClassifierEvaluationProcedure initializing optionHandler with
   * parameters for verbose and time.
   */
  public ClassifierEvaluationProcedure() {
    super();
    addOption(VERBOSE_FLAG);
    addOption(TIME_FLAG);
    addOption(HOLDOUT_PARAM);
  }

  @SuppressWarnings("unchecked")
  public EvaluationResult<O, C> evaluate(Database<O> test, C algorithm) throws IllegalStateException {
    SortedSet<ClassLabel> lbls = DatabaseUtil.getClassLabels(test);
    // todo: ugly cast.
    this.labels = lbls.toArray((L[]) new Object[lbls.size()]);
    this.partition = holdout.partition(test);
    
    // TODO: add support for predefined test and training pairs!
    
    if(partition == null || partition.length < 1) {
      throw new IllegalStateException(ILLEGAL_STATE + " No dataset partition specified.");
    }
    int[][] confusion = new int[labels.length][labels.length];
    for(int p = 0; p < this.partition.length; p++) {
      TrainingAndTestSet<O, L> partition = this.partition[p];
      if(logger.isVerbose()) {
        logger.verbose("building classifier for partition " + (p + 1));
      }
      long buildstart = System.currentTimeMillis();
      algorithm.buildClassifier(partition.getTraining(), labels);
      long buildend = System.currentTimeMillis();
      if(time) {
        logger.verbose("time for building classifier for partition " + (p + 1) + ": " + (buildend - buildstart) + " msec.");
      }
      if(verbose) {
        logger.verbose("evaluating classifier for partition " + (p + 1));
      }
      long evalstart = System.currentTimeMillis();
      for(Iterator<Integer> iter = partition.getTest().iterator(); iter.hasNext();) {
        Integer id = iter.next();
        // TODO: another evaluation could make use of distribution?
        int predicted = algorithm.classify(partition.getTest().get(id));
        int real = Arrays.binarySearch(labels, partition.getTest().getAssociation(AssociationID.CLASS, id));
        confusion[predicted][real]++;
      }
      long evalend = System.currentTimeMillis();
      if(time) {
        logger.verbose("time for evaluating classifier for partition " + (p + 1) + ": " + (evalend - evalstart) + " msec.");
      }
    }
    //if(testSetProvided) {
    //  return new ConfusionMatrixBasedEvaluation<O, L, C>(new ConfusionMatrix(labels, confusion), algorithm, partition[0].getTraining(), partition[0].getTest(), this);
    //}
    //else {
      algorithm.buildClassifier(holdout.completeData(), labels);
      return new ConfusionMatrixBasedEvaluation<O, L, C>(new ConfusionMatrix(labels, confusion), algorithm, holdout.completeData(), null, this);
    //}

  }

  public void setTime(boolean time) {
    this.time = time;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public String parameterDescription() {
    return this.getClass().getName() + " performs a confusion matrix based evaluation.";
  }

  /**
   * Sets parameters time and verbose, if given. Otherwise, the current setting
   * remains unchanged.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    if(TIME_FLAG.isSet()) {
      time = true;
    }
    if(VERBOSE_FLAG.isSet()) {
      verbose = true;
      LoggingConfiguration.setVerbose(true);
    }

    // parameter holdout
    holdout = HOLDOUT_PARAM.instantiateClass();
    remainingParameters = holdout.setParameters(remainingParameters);

    return remainingParameters;
  }

}
