package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Arrays;
import java.util.Set;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClassifierEvaluation<M extends MetricalObject, C extends Classifier<M>> implements EvaluationProcedure<M, C>
{
    private boolean testSetProvided = false;
    
    protected boolean time = false;
    
    protected boolean verbose = false;
    
    
    private Holdout<M> holdout;

    private TrainingAndTestSet<M>[] partition;

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.database.Database)
     */
    public void set(Database<M> training, Database<M> test)
    {
        Set<ClassLabel> labels = Util.getClassLabels(training);
        labels.addAll(Util.getClassLabels(test));
        ClassLabel[] classLabels = labels.toArray(new ClassLabel[labels.size()]);
        Arrays.sort(classLabels);
        this.holdout = null;
        this.testSetProvided = true;
        this.partition = new TrainingAndTestSet[1];
        this.partition[0] = new TrainingAndTestSet<M>(training,test,classLabels);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.evaluation.Holdout)
     */
    public void set(Database<M> data, Holdout<M> holdout)
    {
        this.holdout = holdout;
        this.testSetProvided = false;
        this.partition = holdout.partition(data);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.EvaluationProcedure#evaluate(A)
     */
    public Evaluation<M, C> evaluate(C algorithm) throws IllegalStateException
    {
        
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.EvaluationProcedure#setting()
     */
    public String setting()
    {
        if(testSetProvided)
        {
            return "Test set provided.";
        }
        else
        {
            return "Used holdout: "+holdout.getClass().getName()+"\n"+holdout.getAttributeSettings().toString();
        }
    }

    public void setTime(boolean time)
    {
        this.time = time;
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

}
