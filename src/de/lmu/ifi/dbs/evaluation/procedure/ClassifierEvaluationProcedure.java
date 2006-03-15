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
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;

/**
 * Class to evaluate a classifier using a specified holdout or
 * a provided pair of training and test data.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClassifierEvaluationProcedure<O extends DatabaseObject, C extends Classifier<O>> implements EvaluationProcedure<O, C> {
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
  protected ClassLabel[] labels;

  /**
   * Holds the holdout.
   */
  private Holdout<O> holdout;

  /**
   * Holds the partitions.
   */
  private TrainingAndTestSet<O>[] partition;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler to handle options.
   * optionHandler should be initialized
   * using parameterToDescription in any non-abstract class extending this
   * class.
   */
  protected OptionHandler optionHandler;

  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];
  
  /**
   * Provides a ClassifierEvaluationProcedure
   * initializing optionHandler with
   * parameters for
   * verbose and time.
   */
  public ClassifierEvaluationProcedure() {
    parameterToDescription.put(VERBOSE_F, VERBOSE_D);
    parameterToDescription.put(TIME_F, TIME_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.database.Database)
   */
  public void set(Database<O> training, Database<O> test) {
    SortedSet<ClassLabel> labels = Util.getClassLabels(training);
    labels.addAll(Util.getClassLabels(test));
    this.labels = labels.toArray(new ClassLabel[labels.size()]);
    // not necessary, since Util uses a sorted set now
    // Arrays.sort(this.labels);
    this.holdout = null;
    this.testSetProvided = true;
    //noinspection unchecked
    this.partition = new TrainingAndTestSet[1];
    this.partition[0] = new TrainingAndTestSet<O>(training, test, this.labels);
  }

  /**
   * @see EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.evaluation.holdout.Holdout)
   */
  public void set(Database<O> data, Holdout<O> holdout) {
    SortedSet<ClassLabel> labels = Util.getClassLabels(data);
    this.labels = labels.toArray(new ClassLabel[labels.size()]);
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
      TrainingAndTestSet<O> partition = this.partition[p];
      if (verbose) {
        System.out.println("building classifier for partition " + (p + 1));
      }
      long buildstart = System.currentTimeMillis();
      algorithm.buildClassifier(partition.getTraining(), labels);
      long buildend = System.currentTimeMillis();
      if (time) {
        System.out.println("time for building classifier for partition " + (p + 1) + ": " + (buildend - buildstart) + " msec.");
      }
      if (verbose) {
        System.out.println("evaluating classifier for partition " + (p + 1));
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
        System.out.println("time for evaluating classifier for partition " + (p + 1) + ": " + (evalend - evalstart) + " msec.");
      }
    }
    if (testSetProvided) {
      return new ConfusionMatrixBasedEvaluation<O, C>(new ConfusionMatrix(labels, confusion), algorithm, partition[0].getTraining(), partition[0].getTest(), this);
    }
    else {
      algorithm.buildClassifier(holdout.completeData(), labels);
      return new ConfusionMatrixBasedEvaluation<O, C>(new ConfusionMatrix(labels, confusion), algorithm, holdout.completeData(), null, this);
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
    AttributeSettings settings = new AttributeSettings(this);
    settings.addSetting(VERBOSE_F, Boolean.toString(verbose));
    settings.addSetting(TIME_F, Boolean.toString(time));
    settings.addSetting("holdout", setting());
    List<AttributeSettings> list = new ArrayList<AttributeSettings>(1);
    list.add(settings);
    return list;
  }

  /**
   * Sets parameters time and verbose, if given.
   * Otherwise, the current setting remains unchanged.
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
    setParameters(args, remainingParameters);
    return remainingParameters;
  }
  
  /**
   * Sets the difference of the first array minus the second array
   * as the currently set parameter array.
   * 
   * 
   * @param complete the complete array
   * @param part an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part)
  {
      currentParameterArray = Util.difference(complete, part);
  }
  
  /**
   * 
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters()
  {
      String[] param = new String[currentParameterArray.length];
      System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
      return param;
  }


}
