package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure;

import java.io.PrintStream;

/**
 * Provides the prediction performance measures for a classifier
 * based on the confusion matrix.
 * 
 * @author Arthur Zimek
 */
public class ConfusionMatrixBasedEvaluation<O extends DatabaseObject,L extends ClassLabel<L>, C extends Classifier<O, L>> extends AbstractClassifierEvaluation<O,L,C>
{
    
    /**
     * Holds the confusion matrix.
     */    
    private ConfusionMatrix confusionmatrix;
    
    
    
    /**
     * Holds the used EvaluationProcedure.
     */
    private EvaluationProcedure<O,C,L> evaluationProcedure;

    /**
     * Provides an evaluation based on the given confusion matrix.
     * 
     * @param confusionmatrix the confusion matrix to provide
     * the prediction performance measures for
     * @param classifier the classifier this evaluation is based on
     * @param database the training set this evaluation is based on
     * @param testset the test set this evaluation is based on
     * @param evaluationProcedure the evaluation procedure used
     */
    public ConfusionMatrixBasedEvaluation(ConfusionMatrix confusionmatrix, C classifier, Database<O> database, Database<O> testset, EvaluationProcedure<O,C,L> evaluationProcedure)
    {
        super(database,testset,classifier);
        this.confusionmatrix = confusionmatrix;
        this.evaluationProcedure = evaluationProcedure;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.Evaluation#outputEvaluationResult(java.io.PrintStream)
     */
    public void outputEvaluationResult(PrintStream out)
    {
        out.println("\nEvaluation:");
        out.println(evaluationProcedure.getClass().getName());
        out.println(evaluationProcedure.setting());
        out.print("\nAccuracy: \n  correctly classified instances: ");
        out.println(confusionmatrix.truePositives());
        out.print("true positive rate:         ");
        double tpr = confusionmatrix.truePositiveRate();
        out.println(tpr);
        out.print("false positive rate:        ");
        out.println(confusionmatrix.falsePositiveRate());
        out.print("positive predicted value:   ");
        double ppv = confusionmatrix.positivePredictedValue(); 
        out.println(ppv);
        out.print("F1-measure:                 ");
        out.println((2*ppv*tpr) / (ppv+tpr));
        out.println("\nconfusion matrix:\n");
        out.println(confusionmatrix.toString());
    }

}
