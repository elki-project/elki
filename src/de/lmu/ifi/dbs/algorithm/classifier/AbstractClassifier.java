package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.evaluation.ClassifierEvaluation;
import de.lmu.ifi.dbs.evaluation.Evaluation;
import de.lmu.ifi.dbs.evaluation.Holdout;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * An abstract classifier already based on AbstractAlgorithm
 * making use of settings for time and verbose.
 * Furthermore, any classifier is given an evaluation procedure.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractClassifier<M extends MetricalObject> extends AbstractAlgorithm<M> implements Classifier<M>
{

    /**
     * The association id for the class label.
     */
    public static final AssociationID CLASS = AssociationID.CLASS;
    
    protected ClassifierEvaluation<M,Classifier<M>> evaluationProcedure;
    
    protected Holdout<M> holdout;
    
    private Evaluation<M,Classifier<M>> evaluationResult;
    
    /**
     * Holds the available labels.
     * Should be set by the training method
     * {@link Classifier#buildClassifier(Database) buildClassifier(Database)}.
     */
    protected ClassLabel[] labels = new ClassLabel[0];

    /**
     * Sets parameter settings as AbstractAlgorithm.
     */
    protected AbstractClassifier()
    {
        super();
    }

    /**
     * Evaluates this algorithm on the given database
     * using the currently set evaluation procedure and
     * holdout. The result of the evaluation procedure
     * is provided as result of this algorithm.
     * The time for the complete evaluation is given
     * if the flag time is set.
     * Whether to assess time and give verbose comments
     * in single evaluation steps is passed
     * to the evaluation procedure matching the
     * setting of the flags time and verbose. 
     * 
     * @param database the database to build the model on
     * @throws IllegalStateException if the classifier is not properly initiated (e.g. parameters are not set)
     */
    @Override
    public final void runInTime(Database<M> database) throws IllegalStateException
    {
        evaluationProcedure.setTime(this.isTime());
        evaluationProcedure.setVerbose(this.isVerbose());
        evaluationProcedure.set(database,holdout);
        evaluationResult = evaluationProcedure.evaluate(this);
    }


    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result<M> getResult()
    {
        return evaluationResult;
    }

    /**
     * Provides a classification for a given instance.
     * The classification is the index of the class-label
     * in {@link #labels labels}.
     * 
     * This method returns the index of the maximum probability
     * as provided by {@link #classDistribution(M) classDistribution(M)}.
     * If an extending classifier requires a different classification,
     * it should overwrite this method.
     * 
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public int classify(M instance) throws IllegalStateException
    {
        return Util.getIndexOfMaximum(classDistribution(instance));
    }


    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#getClassLabel(int)
     */
    public final ClassLabel getClassLabel(int index) throws IllegalArgumentException
    {
        try
        {
            return labels[index];
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            IllegalArgumentException iae = new IllegalArgumentException("Invalid class index.",e);
            iae.fillInStackTrace();
            throw iae;
        }
    }
    

    
}
