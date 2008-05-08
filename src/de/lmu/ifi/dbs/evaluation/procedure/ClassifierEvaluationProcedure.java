package de.lmu.ifi.dbs.evaluation.procedure;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.evaluation.ConfusionMatrix;
import de.lmu.ifi.dbs.evaluation.ConfusionMatrixBasedEvaluation;
import de.lmu.ifi.dbs.evaluation.Evaluation;
import de.lmu.ifi.dbs.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.evaluation.holdout.TrainingAndTestSet;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Class to evaluate a classifier using a specified holdout or a provided pair
 * of training and test data.
 *
 * @author Arthur Zimek 
 */
public class ClassifierEvaluationProcedure<O extends DatabaseObject, L extends ClassLabel<L>,C extends Classifier<O, L>> extends AbstractParameterizable implements EvaluationProcedure<O, C, L> {

  /**
   * Holds whether a test set hs been provided.
   */
  private boolean testSetProvided = false;

  /**
   * Flag for time assessment.
   */
  public static final String TIME_F = "time";

  /**
   * Description for flag time.
   */
  public static final String TIME_D = "flag whether to assess time";

  /**
   * Flag for request of verbose messages.
   */
  public static final String VERBOSE_F = "verbose";

  /**
   * Description for flag verbose.
   */
  public static final String VERBOSE_D = "flag to request verbose messages during evaluation";

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
  private Holdout<O,L> holdout;

  /**
   * Holds the partitions.
   */
  private TrainingAndTestSet<O,L>[] partition;

  /**
   * Provides a ClassifierEvaluationProcedure initializing optionHandler with
   * parameters for verbose and time.
   */
  public ClassifierEvaluationProcedure() {
    super();
    optionHandler.put(new Flag(VERBOSE_F, VERBOSE_D));
    optionHandler.put(new Flag(TIME_F, TIME_D));
  }

  /**
   * @see EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database,
   *      de.lmu.ifi.dbs.database.Database)
   */
  public void set(Database<O> training, Database<O> test) {
    SortedSet<ClassLabel<?>> labels =  Util.getClassLabels(training);
    labels.addAll(Util.getClassLabels(test));
    this.labels = labels.toArray((L[])new Object[labels.size()]);
    // not necessary, since Util uses a sorted set now
    // Arrays.sort(this.labels);
    this.holdout = null;
    this.testSetProvided = true;
    // noinspection unchecked
    this.partition = new TrainingAndTestSet[1];
    this.partition[0] = new TrainingAndTestSet<O,L>(training, test, this.labels);
  }

  /**
   * @see EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database,
   *      de.lmu.ifi.dbs.evaluation.holdout.Holdout)
   */
  public void set(Database<O> data, Holdout<O,L> holdout) {
    SortedSet<ClassLabel<?>> labels =  Util.getClassLabels(data);
    this.labels = labels.toArray((L[])new Object[labels.size()]);
    // not necessary, since Util uses a sorted set now
    // Arrays.sort(this.labels);

    this.holdout = holdout;
    this.testSetProvided = false;
    this.partition = holdout.partition(data);
  }

  /**
   * @see EvaluationProcedure#evaluate(de.lmu.ifi.dbs.algorithm.Algorithm)
   */
  public Evaluation<O, C> evaluate(C algorithm) throws IllegalStateException {
    if (partition == null || partition.length < 1) {
      throw new IllegalStateException(ILLEGAL_STATE + " No dataset partition specified.");
    }
    int[][] confusion = new int[labels.length][labels.length];
    for (int p = 0; p < this.partition.length; p++) {
      TrainingAndTestSet<O,L> partition = this.partition[p];
      if (verbose) {
        verbose("building classifier for partition " + (p + 1));
      }
      long buildstart = System.currentTimeMillis();
      algorithm.buildClassifier(partition.getTraining(), labels);
      long buildend = System.currentTimeMillis();
      if (time) {
        verbose("time for building classifier for partition " + (p + 1) + ": " + (buildend - buildstart) + " msec.");
      }
      if (verbose) {
        verbose("evaluating classifier for partition " + (p + 1));
      }
      long evalstart = System.currentTimeMillis();
      for (Iterator<Integer> iter = partition.getTest().iterator(); iter.hasNext();) {
        Integer id = iter.next();
        // TODO: another evaluation could make use of distribution?
        int predicted = algorithm.classify(partition.getTest().get(id));
        int real = Arrays.binarySearch(labels, partition.getTest().getAssociation(AssociationID.CLASS, id));
        confusion[predicted][real]++;
      }
      long evalend = System.currentTimeMillis();
      if (time) {
        verbose("time for evaluating classifier for partition " + (p + 1) + ": " + (evalend - evalstart) + " msec.");
      }
    }
    if (testSetProvided) {
    	//noinspection unchecked
      return new ConfusionMatrixBasedEvaluation(new ConfusionMatrix(labels, confusion), algorithm, partition[0].getTraining(), partition[0].getTest(), this);
    }
    else {
      algorithm.buildClassifier(holdout.completeData(), labels);
      //noinspection unchecked
      return new ConfusionMatrixBasedEvaluation(new ConfusionMatrix(labels, confusion), algorithm, holdout.completeData(), null, this);
    }

  }

  /**
   * @see EvaluationProcedure#setting()
   */
  public String setting() {
    if (testSetProvided) {
      return "Test set provided.";
    }
    else {
      return "Used holdout: " + holdout.getClass().getName() + "\n" + holdout.getAttributeSettings().toString();
    }
  }

  /**
   * @see EvaluationProcedure#setTime(boolean)
   */
  public void setTime(boolean time) {
    this.time = time;
  }

  /**
   * @see EvaluationProcedure#setVerbose(boolean)
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return this.getClass().getName() + " performs a confusion matrix based evaluation.";
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
		AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting("holdout", setting());
    return attributeSettings;
  }

  /**
   * Sets parameters time and verbose, if given. Otherwise, the current
   * setting remains unchanged.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    if (optionHandler.isSet(TIME_F)) {
      time = true;
    }
    if (optionHandler.isSet(VERBOSE_F)) {
      verbose = true;
    }

    return remainingParameters;
  }

}
