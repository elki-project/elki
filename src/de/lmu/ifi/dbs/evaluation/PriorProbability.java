package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;

import java.io.PrintStream;

/**
 * The prior probability reflects the apriori probability of
 * all classes as their relative abundance in the database.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PriorProbability<O extends DatabaseObject, C extends Classifier<O>> extends NullModel<O,C>
{
    /**
     * Holds the prior probability.
     */
    protected double[] priorProbability;

    /**
     * Provides an evaluation simply reflecting the prior probability.
     * 
     * @param db the database the apriori probability is based on
     * @param classifier the classifier related to this evaluation
     * @param labels the class labels
     * @param priorProbability the relative abundance of all classes
     */
    public PriorProbability(Database<O> db, Database<O> testset, C classifier, ClassLabel[] labels, double[] priorProbability)
    {
        super(db, testset, classifier, labels);
        this.priorProbability = new double[priorProbability.length];
        System.arraycopy(priorProbability,0,this.priorProbability,0,priorProbability.length);
    }
    
    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.evaluation.Evaluation#outputEvaluationResult(java.io.PrintStream)
     */
    public void outputEvaluationResult(PrintStream output)
    {
        output.print("### prior probabilities for classes:\n### ");
        for(int i = 0; i < priorProbability.length; i++)
        {
            output.print(labels.get(i));
            output.print(" : ");
            output.println(priorProbability[i]);            
        }
        output.println();
    }
}
