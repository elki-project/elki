package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;

import java.io.PrintStream;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PriorProbability<M extends MetricalObject, C extends Classifier<M>> extends NullModel<M,C>
{
    protected double[] priorProbability;

    /**
     * @param db
     * @param labels
     * @param priorProbability
     */
    public PriorProbability(Database<M> db, C classifier, ClassLabel[] labels, double[] priorProbability)
    {
        super(db, classifier, labels);
        this.priorProbability = new double[priorProbability.length];
        System.arraycopy(priorProbability,0,this.priorProbability,0,priorProbability.length);
    }
    
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
