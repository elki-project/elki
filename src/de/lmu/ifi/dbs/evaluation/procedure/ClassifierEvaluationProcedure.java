package de.lmu.ifi.dbs.evaluation.procedure;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.evaluation.ConfusionMatrix;
import de.lmu.ifi.dbs.evaluation.ConfusionMatrixBasedEvaluation;
import de.lmu.ifi.dbs.evaluation.Evaluation;
import de.lmu.ifi.dbs.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.evaluation.holdout.TrainingAndTestSet;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Class to evaluate a classifier using a specified holdout or
 * a provided pair of training and test data.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClassifierEvaluationProcedure<M extends MetricalObject, C extends Classifier<M>> implements EvaluationProcedure<M, C>
{
    /**
     * Holds whether a test set hs been provided.
     */
    private boolean testSetProvided = false;
    
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
    private Holdout<M> holdout;

    /**
     * Holds the partitions.
     */
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
        if(partition==null || partition.length<1)
        {
            throw new IllegalStateException(ILLEGAL_STATE+" No dataset partition specified.");
        }
        int[][] confusion = new int[labels.length][labels.length];
        // TODO verbose & time
        for(TrainingAndTestSet<M> partition : this.partition)
        {
            algorithm.buildClassifier(partition.getTraining(),labels);
            for(Iterator<Integer> iter = partition.getTest().iterator(); iter.hasNext();)
            {
                Integer id = iter.next();
                // TODO: another evaluation could make use of distribution?
                int predicted = algorithm.classify(partition.getTest().get(id));
                int real = Arrays.binarySearch(labels,partition.getTest().getAssociation(AssociationID.CLASS,id));
                confusion[predicted][real]++;
            }
        }
        if(testSetProvided)
        {
            return new ConfusionMatrixBasedEvaluation<M,C>(new ConfusionMatrix(labels,confusion),algorithm,partition[0].getTraining(),partition[0].getTest(),this);
        }
        else
        {
            algorithm.buildClassifier(holdout.completeData(),labels);
            return new ConfusionMatrixBasedEvaluation<M,C>(new ConfusionMatrix(labels,confusion),algorithm,holdout.completeData(),null,this);
        }
        
        
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

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure#setTime(boolean)
     */
    public void setTime(boolean time)
    {
        this.time = time;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure#setVerbose(boolean)
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

}
