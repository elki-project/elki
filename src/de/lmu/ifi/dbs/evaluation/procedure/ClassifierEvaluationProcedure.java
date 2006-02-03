package de.lmu.ifi.dbs.evaluation.procedure;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.evaluation.Evaluation;
import de.lmu.ifi.dbs.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.evaluation.holdout.TrainingAndTestSet;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Arrays;
import java.util.Set;

/**
 * Class to evaluate a classifier using a specified holdout or
 * a provided pair of training and test data.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClassifierEvaluationProcedure<M extends MetricalObject, C extends Classifier<M>> implements EvaluationProcedure<M, C>
{
    private boolean testSetProvided = false;
    
    protected boolean time = false;
    
    protected boolean verbose = false;
    
    protected ClassLabel[] labels;
    
    private Holdout<M> holdout;

    private TrainingAndTestSet<M>[] partition;

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.database.Database)
     */
    public void set(Database<M> training, Database<M> test)
    {
        Set<ClassLabel> labels = Util.getClassLabels(training);
        labels.addAll(Util.getClassLabels(test));
        this.labels = labels.toArray(new ClassLabel[labels.size()]);
        Arrays.sort(this.labels);
        this.holdout = null;
        this.testSetProvided = true;
        this.partition = new TrainingAndTestSet[1];
        this.partition[0] = new TrainingAndTestSet<M>(training,test,this.labels);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.evaluation.holdout.Holdout)
     */
    public void set(Database<M> data, Holdout<M> holdout)
    {
        Set<ClassLabel> labels = Util.getClassLabels(data);
        this.labels = labels.toArray(new ClassLabel[labels.size()]);
        Arrays.sort(this.labels);
        
        this.holdout = holdout;
        this.testSetProvided = false;
        this.partition = holdout.partition(data);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure#evaluate(A)
     */
    public Evaluation<M, C> evaluate(C algorithm) throws IllegalStateException
    {
        int[][] confusion = new int[labels.length][labels.length];
        
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure#setting()
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
