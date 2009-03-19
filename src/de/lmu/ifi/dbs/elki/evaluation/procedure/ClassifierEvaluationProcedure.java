package de.lmu.ifi.dbs.elki.evaluation.procedure;

import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.ConfusionMatrix;
import de.lmu.ifi.dbs.elki.evaluation.ConfusionMatrixBasedEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.EvaluationResult;
import de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.elki.evaluation.holdout.TrainingAndTestSet;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

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
public class ClassifierEvaluationProcedure<O extends DatabaseObject, L extends ClassLabel, C extends Classifier<O, L, Result>> extends AbstractParameterizable implements EvaluationProcedure<O, L, C> {

    /**
     * Holds whether a test set has been provided.
     */
    private boolean testSetProvided = false;

    /**
     * Flag to request verbose information.
     */
    private final Flag TIME_FLAG = new Flag(OptionID.ALGORITHM_TIME);

    /**
     * Flag to request verbose information.
     */
    private final Flag VERBOSE_FLAG = new Flag(OptionID.ALGORITHM_VERBOSE);

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
    }

    @SuppressWarnings("unchecked")
    public void set(Database<O> training, Database<O> test) {
        SortedSet<ClassLabel> labels = DatabaseUtil.getClassLabels(training);
        labels.addAll(DatabaseUtil.getClassLabels(test));
        // TODO: ugly cast.
        this.labels = labels.toArray((L[]) new Object[labels.size()]);
        this.holdout = null;
        this.testSetProvided = true;
        this.partition = new TrainingAndTestSet[1];
        this.partition[0] = new TrainingAndTestSet<O, L>(training, test, this.labels);
    }

    @SuppressWarnings("unchecked")
    public void set(Database<O> data, Holdout<O, L> holdout) {
        SortedSet<ClassLabel> labels = DatabaseUtil.getClassLabels(data);
        // TODO: ugly cast.
        this.labels = labels.toArray((L[]) new Object[labels.size()]);

        this.holdout = holdout;
        this.testSetProvided = false;
        this.partition = holdout.partition(data);
    }

    public EvaluationResult<O, C> evaluate(C algorithm) throws IllegalStateException {
        if (partition == null || partition.length < 1) {
            throw new IllegalStateException(ILLEGAL_STATE + " No dataset partition specified.");
        }
        int[][] confusion = new int[labels.length][labels.length];
        for (int p = 0; p < this.partition.length; p++) {
            TrainingAndTestSet<O, L> partition = this.partition[p];
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
            return new ConfusionMatrixBasedEvaluation<O, L, C>(new ConfusionMatrix(labels, confusion), algorithm, partition[0].getTraining(), partition[0].getTest(), this);
        }
        else {
            algorithm.buildClassifier(holdout.completeData(), labels);
            return new ConfusionMatrixBasedEvaluation<O, L, C>(new ConfusionMatrix(labels, confusion), algorithm, holdout.completeData(), null, this);
        }

    }

    public String setting() {
        if (testSetProvided) {
            return "Test set provided.";
        }
        else {
            return "Used holdout: " + holdout.getClass().getName() + "\n" + holdout.getAttributeSettings().toString();
        }
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
     * Calls the super method
     * and adds to the returned attribute a description of the used holdout.
     *
     * @see #setting
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        AttributeSettings mySettings = attributeSettings.get(0);
        mySettings.addSetting("holdout", setting());
        return attributeSettings;
    }

    /**
     * Sets parameters time and verbose, if given. Otherwise, the current
     * setting remains unchanged.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = optionHandler.grabOptions(args);

        if (TIME_FLAG.isSet())
            time = true;
        if (VERBOSE_FLAG.isSet())
            verbose = true;

        return remainingParameters;
    }

}
