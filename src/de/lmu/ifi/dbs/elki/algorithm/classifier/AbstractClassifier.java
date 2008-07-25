package de.lmu.ifi.dbs.elki.algorithm.classifier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluation;
import de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.elki.evaluation.holdout.StratifiedCrossValidation;
import de.lmu.ifi.dbs.elki.evaluation.procedure.ClassifierEvaluationProcedure;
import de.lmu.ifi.dbs.elki.evaluation.procedure.EvaluationProcedure;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

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
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.classifier.AbstractClassifier#EVALUATION_PROCEDURE_PARAM}
     */
    public static final OptionID EVALUATION_PROCEDURE_ID = OptionID.getOrCreateOptionID(
        "classifier.eval",
        "Classname of the evaluation-procedure to use for evaluation " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(EvaluationProcedure.class) + "."
    );

    /**
     * Parameter to specify the evaluation-procedure to use for evaluation,
     * must extend {@link EvaluationProcedure}.
     * <p>Key: {@code -classifier.eval} </p>
     * <p>Default value: {@link ClassifierEvaluationProcedure} </p>
     */
    private final ClassParameter<EvaluationProcedure> EVALUATION_PROCEDURE_PARAM =
        new ClassParameter<EvaluationProcedure>(EVALUATION_PROCEDURE_ID,
            EvaluationProcedure.class, ClassifierEvaluationProcedure.class.getName());

    /**
     * Holds the value of {@link #EVALUATION_PROCEDURE_PARAM}.
     */
    protected EvaluationProcedure<O, Classifier<O, L>, L> evaluationProcedure;

    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.classifier.AbstractClassifier#HOLDOUT_PARAM}
     */
    public static final OptionID HOLDOUT_ID = OptionID.getOrCreateOptionID(
        "classifier.holdout",
        "<Classname of the holdout for evaluation  " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Holdout.class) + "."
    );

    /**
     * Parameter to specify the holdout for evaluation,
     * must extend {@link de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout}.
     * <p>Key: {@code -classifier.holdout} </p>
     * <p>Default value: {@link StratifiedCrossValidation} </p>
     */
    private final ClassParameter<Holdout> HOLDOUT_PARAM =
        new ClassParameter<Holdout>(EVALUATION_PROCEDURE_ID,
            Holdout.class, StratifiedCrossValidation.class.getName());

    /**
     * Holds the value of {@link #HOLDOUT_PARAM}.
     */
    protected Holdout<O, L> holdout;

    /**
     * The result.
     */
    private Evaluation<O, Classifier<O, L>> evaluationResult;

    /**
     * Holds the available labels. Should be set by the training method
     * {@link Classifier#buildClassifier(de.lmu.ifi.dbs.elki.database.Database,de.lmu.ifi.dbs.elki.data.ClassLabel[])}
     */
    @SuppressWarnings("unchecked")
    private L[] labels = (L[]) new ClassLabel[0];

    /**
     * Adds parameters {@link #EVALUATION_PROCEDURE_PARAM} and {@link #HOLDOUT_PARAM}
     * to the option handler additionally to parameters of super class.
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
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult()
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
     * Calls {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
     * and instantiates {@link #evaluationProcedure} according to the value of parameter {@link #EVALUATION_PROCEDURE_PARAM}
     * and {@link #holdout} according to the value of parameter {@link #HOLDOUT_PARAM}.
     * The remaining parameters are passed to the {@link #evaluationProcedure}
     * and, then, to the {@link #holdout}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // parameter evaluation procedure
        // noinspection unchecked
        evaluationProcedure = EVALUATION_PROCEDURE_PARAM.instantiateClass();
        evaluationProcedure.setTime(isTime());
        evaluationProcedure.setVerbose(isVerbose());
        remainingParameters = evaluationProcedure.setParameters(remainingParameters);

        // parameter holdout
        // noinspection unchecked
        holdout = HOLDOUT_PARAM.instantiateClass();
        remainingParameters = holdout.setParameters(remainingParameters);

        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#getAttributeSettings()}
     * and adds to the returned attribute settings the attribute settings of
     * {@link #evaluationProcedure} and {@link #holdout}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();

        attributeSettings.addAll(evaluationProcedure.getAttributeSettings());
        attributeSettings.addAll(holdout.getAttributeSettings());

        return attributeSettings;
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#parameterDescription()}
     * and appends the parameter description of
     * {@link #evaluationProcedure} and {@link #holdout} if they are already initialized.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());
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
