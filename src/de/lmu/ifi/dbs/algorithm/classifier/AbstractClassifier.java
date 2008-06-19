package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.evaluation.Evaluation;
import de.lmu.ifi.dbs.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.evaluation.holdout.StratifiedCrossValidation;
import de.lmu.ifi.dbs.evaluation.procedure.ClassifierEvaluationProcedure;
import de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.Arrays;
import java.util.List;

/**
 * An abstract classifier already based on AbstractAlgorithm making use of
 * settings for time and verbose. Furthermore, any classifier is given an
 * evaluation procedure.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
public abstract class AbstractClassifier<O extends DatabaseObject, L extends ClassLabel<L>> extends AbstractAlgorithm<O> implements Classifier<O, L> {
    /**
     * The association id for the class label.
     */
    protected static final AssociationID CLASS = AssociationID.CLASS;

    /**
     * Parameter to specify the evaluation-procedure to use for evaluation,
     * must extend {@link EvaluationProcedure}.
     * <p>Key: {@code -classifier.eval} </p>
     * <p>Default value: {@link ClassifierEvaluationProcedure} </p>
     */
    private final ClassParameter<EvaluationProcedure> EVALUATION_PROCEDURE_PARAM =
        new ClassParameter<EvaluationProcedure>(OptionID.CLASSIFIER_EVALUATION_PROCEDURE,
            EvaluationProcedure.class, ClassifierEvaluationProcedure.class.getName());

    /**
     * Parameter to specify the holdout for evaluation,
     * must extend {@link de.lmu.ifi.dbs.evaluation.holdout.Holdout}.
     * <p>Key: {@code -classifier.holdout} </p>
     * <p>Default value: {@link StratifiedCrossValidation} </p>
     */
    private final ClassParameter<Holdout> HOLDOUT_PARAM =
        new ClassParameter<Holdout>(OptionID.CLASSIFIER_EVALUATION_PROCEDURE,
            Holdout.class, StratifiedCrossValidation.class.getName());

    /**
     * Holds the value of parameter evaluation procedure.
     */
    protected EvaluationProcedure<O, Classifier<O, L>, L> evaluationProcedure;

    /**
     * Holds the value of parameter holdout.
     */
    protected Holdout<O, L> holdout;

    /**
     * The result.
     */
    private Evaluation<O, Classifier<O, L>> evaluationResult;

    /**
     * Holds the available labels. Should be set by the training method
     * {@link Classifier#buildClassifier(de.lmu.ifi.dbs.database.Database,de.lmu.ifi.dbs.data.ClassLabel[])}
     */
    @SuppressWarnings("unchecked")
    private L[] labels = (L[]) new ClassLabel[0];

    /**
     * Sets parameter settings as AbstractAlgorithm.
     */
    protected AbstractClassifier() {
        super();

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
    protected final void runInTime(Database<O> database) throws IllegalStateException {
        evaluationProcedure.setTime(this.isTime());
        evaluationProcedure.setVerbose(this.isVerbose());
        evaluationProcedure.set(database, holdout);

        long starteval = System.currentTimeMillis();
        evaluationResult = evaluationProcedure.evaluate(this);
        long endeval = System.currentTimeMillis();
        if (this.isTime()) {
            verbose("time required for evaluation: " + (endeval - starteval) + " msec.");
        }
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public final Result<O> getResult() {
        return evaluationResult;
    }

    /**
     * Provides a classification for a given instance. The classification is the
     * index of the class-label in {@link #labels labels}. <p/> This method
     * returns the index of the maximum probability as provided by
     * {@link #classDistribution(DatabaseObject)}. If an extending classifier
     * requires a different classification, it should overwrite this method.
     *
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized or properly
     *                               trained
     */
    public int classify(O instance) throws IllegalStateException {
        return Util.getIndexOfMaximum(classDistribution(instance));
    }

    /**
     * @see Classifier#getClassLabel(int)
     */
    public final L getClassLabel(int index) throws IllegalArgumentException {
        try {
            return labels[index];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            IllegalArgumentException iae = new IllegalArgumentException("Invalid class index.", e);
            iae.fillInStackTrace();
            throw iae;
        }
    }

    /**
     * Sets the parameters evaluationProcedure and holdout. Passes the remaining
     * parameters to the set evaluation procedure and, then, to the set holdout.
     *
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // parameter evaluation procedure
        String evaluationProcedureClass = getParameterValue(EVALUATION_PROCEDURE_PARAM);
        try {
            // noinspection unchecked
            evaluationProcedure = Util.instantiate(EvaluationProcedure.class, evaluationProcedureClass);
        }
        catch (UnableToComplyException e) {
            throw new WrongParameterValueException(EVALUATION_PROCEDURE_PARAM.getName(),
                evaluationProcedureClass, EVALUATION_PROCEDURE_PARAM.getDescription(), e);
        }
        evaluationProcedure.setTime(isTime());
        evaluationProcedure.setVerbose(isVerbose());
        remainingParameters = evaluationProcedure.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        // parameter holdout
        String holdoutClass = getParameterValue(HOLDOUT_PARAM);
        try {
            // noinspection unchecked
            holdout = Util.instantiate(Holdout.class, holdoutClass);
        }
        catch (UnableToComplyException e) {
            throw new WrongParameterValueException(HOLDOUT_PARAM.getName(),
                holdoutClass, HOLDOUT_PARAM.getDescription(), e);
        }
        remainingParameters = holdout.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();

        attributeSettings.addAll(evaluationProcedure.getAttributeSettings());
        attributeSettings.addAll(holdout.getAttributeSettings());

        return attributeSettings;
    }

    /**
     * Returns the class labels as currently set.
     *
     * @return the class labels
     */
    public final L[] getLabels() {
        return this.labels;
    }

    /**
     * Sets the given class labels as class labels to use. The given array gets
     * sorted by the setting method.
     *
     * @param labels the labels to use for building the classifier
     */
    public final void setLabels(L[] labels) {
        this.labels = labels;
        Arrays.sort(this.labels);
    }

}
