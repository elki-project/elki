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
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.List;

/**
 * An abstract classifier already based on AbstractAlgorithm
 * making use of settings for time and verbose.
 * Furthermore, any classifier is given an evaluation procedure.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractClassifier<O extends DatabaseObject> extends AbstractAlgorithm<O> implements Classifier<O>
{

    /**
     * The association id for the class label.
     */
    public static final AssociationID CLASS = AssociationID.CLASS;
    
    /**
     * The default evaluation procedure.
     * 
     */
    public static final String DEFAULT_EVALUATION_PROCEDURE = ClassifierEvaluationProcedure.class.getName();
    
    /**
     * The evaluation procedure.
     */
    protected ClassifierEvaluationProcedure<O,Classifier<O>> evaluationProcedure;
    
    /**
     * The parameter for the evaluation procedure.
     */
    public final String EVALUATION_PROCEDURE_P = "eval";
    
    /**
     * The description for parameter evaluation procedure.
     */
    public final String EVALUATION_PROCEDURE_D = "<class>the evaluation-procedure to use for evaluation - must extend "+ClassifierEvaluationProcedure.class.getName()+". Default: "+DEFAULT_EVALUATION_PROCEDURE+".";
    
    /**
     * The holdout used for evaluation.
     */
    protected Holdout<O> holdout;
    
    /**
     * The default holdout.
     */
    public static final String DEFAULT_HOLDOUT = StratifiedCrossValidation.class.getName();
    
    /**
     * The parameter for the holdout.
     */
    public static final String HOLDOUT_P = "holdout";
    
    /**
     * Description for parameter holdout.
     */
    public static final String HOLDOUT_D = "<class>The holdout for evaluation - must implement "+Holdout.class.getName()+". Default: "+DEFAULT_HOLDOUT+".";
    
    /**
     * The result.
     */
    private Evaluation<O,Classifier<O>> evaluationResult;
    
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
        parameterToDescription.put(EVALUATION_PROCEDURE_P+OptionHandler.EXPECTS_VALUE, EVALUATION_PROCEDURE_D);
        parameterToDescription.put(HOLDOUT_P+OptionHandler.EXPECTS_VALUE, HOLDOUT_D);
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
    protected final void runInTime(Database<O> database) throws IllegalStateException
    {
        evaluationProcedure.setTime(this.isTime());
        evaluationProcedure.setVerbose(this.isVerbose());
        evaluationProcedure.set(database,holdout);
        
        long starteval = System.currentTimeMillis();
        evaluationResult = evaluationProcedure.evaluate(this);
        long endeval = System.currentTimeMillis();
        if(this.isTime())
        {
            System.out.println("time required for evaluation: "+(endeval-starteval)+" msec.");
        }
    }


    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public final Result<O> getResult()
    {
        return evaluationResult;
    }

    /**
     * Provides a classification for a given instance.
     * The classification is the index of the class-label
     * in {@link #labels labels}.
     * 
     * This method returns the index of the maximum probability
     * as provided by {@link #classDistribution(O) classDistribution(M)}.
     * If an extending classifier requires a different classification,
     * it should overwrite this method.
     * 
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public int classify(O instance) throws IllegalStateException
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

    /**
     * Sets the parameters evaluationProcedure and holdout.
     * Passes the remaining parameters to the set evaluation procedure
     * and, then, to the set holdout.
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(EVALUATION_PROCEDURE_P))
        {
            try
            {
                evaluationProcedure = (ClassifierEvaluationProcedure<O, Classifier<O>>) Class.forName(optionHandler.getOptionValue(EVALUATION_PROCEDURE_P)).newInstance();
            }
            catch(UnusedParameterException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(NoParameterValueException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(InstantiationException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(IllegalAccessException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(ClassNotFoundException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }            
        }
        else
        {
            try
            {
                evaluationProcedure = (ClassifierEvaluationProcedure<O, Classifier<O>>) Class.forName(DEFAULT_EVALUATION_PROCEDURE).newInstance();
            }
            catch(InstantiationException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(IllegalAccessException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(ClassNotFoundException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
        }
        if(optionHandler.isSet(HOLDOUT_P))
        {
            try
            {
                holdout = (Holdout<O>) Class.forName(optionHandler.getOptionValue(HOLDOUT_P)).newInstance();
            }
            catch(UnusedParameterException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(NoParameterValueException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(InstantiationException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(IllegalAccessException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(ClassNotFoundException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
        }
        else
        {
            try
            {
                holdout = (Holdout<O>) Class.forName(DEFAULT_HOLDOUT).newInstance();
            }
            catch(InstantiationException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(IllegalAccessException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(ClassNotFoundException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
        }
        remainingParameters = evaluationProcedure.setParameters(remainingParameters);
        remainingParameters = holdout.setParameters(remainingParameters);
        return remainingParameters;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        AttributeSettings setting = settings.get(0);
        setting.addSetting(EVALUATION_PROCEDURE_P, evaluationProcedure.getClass().getName());
        setting.addSetting(HOLDOUT_P, holdout.getClass().getName());
        settings.addAll(evaluationProcedure.getAttributeSettings());
        settings.addAll(holdout.getAttributeSettings());
        return settings;
    }
    

    
}
