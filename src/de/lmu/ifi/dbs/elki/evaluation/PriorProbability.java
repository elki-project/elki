package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;

import java.io.PrintStream;

/**
 * The prior probability reflects the apriori probability of
 * all classes as their relative abundance in the database.
 *
 * @author Arthur Zimek
 */
public class PriorProbability<O extends DatabaseObject, L extends ClassLabel, C extends Classifier<O, L>> extends NullModel<O, L, C> {
    /**
     * Holds the prior probability.
     */
    protected double[] priorProbability;

    /**
     * Provides an evaluation simply reflecting the prior probability.
     *
     * @param db               the database the apriori probability is based on
     * @param classifier       the classifier related to this evaluation
     * @param labels           the class labels
     * @param priorProbability the relative abundance of all classes
     */
    public PriorProbability(Database<O> db, Database<O> testset, C classifier, L[] labels, double[] priorProbability) {
        super(db, testset, classifier, labels);
        this.priorProbability = new double[priorProbability.length];
        System.arraycopy(priorProbability, 0, this.priorProbability, 0, priorProbability.length);
    }

    @Override
    public void outputEvaluationResult(PrintStream output) {
        output.print("### prior probabilities for classes:\n### ");
        for (int i = 0; i < priorProbability.length; i++) {
            output.print(labels.get(i));
            output.print(" : ");
            output.println(priorProbability[i]);
        }
        output.println();
    }
}
