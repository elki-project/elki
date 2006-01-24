package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClassifierEvaluation<M extends MetricalObject, C extends Classifier<M>> implements EvaluationProcedure<M, C>
{
    private boolean testSetProvided = false;
    
    private Holdout<M> holdout;

    private TrainingAndTestSet<M>[] partition;

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.EvaluationProcedure#set(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.database.Database)
     */
    public void set(Database<M> training, Database<M> test)
    {
        this.holdout = null;
        this.testSetProvided = true;
        this.partition = new TrainingAndTestSet[1];
        this.partition[0] = new TrainingAndTestSet<M>(training,test);
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

}
