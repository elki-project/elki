package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A model describing the database and the available class labels.
 * As an empty model this model may be suitable for lazy learners.
 * XXX probably no need for this class
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class NullModel<O extends DatabaseObject,C extends Classifier<O>> extends AbstractClassifierEvaluation<O,C>
{
    /**
     * The labels available for classification.
     */
    protected List<ClassLabel> labels;

    /**
     * Provides a new NullModel for the given database and labels.
     * 
     * @param db the database where the NullModel is bsaed on
     * @param classifier the classifier related to the null model
     * @param labels the labels available for classification
     */
    public NullModel(Database<O> db, Database<O> testset, C classifier, ClassLabel[] labels)
    {
        super(db,testset,classifier);
        this.labels = new ArrayList<ClassLabel>(labels.length);
        for(ClassLabel label : labels)
        {
            this.labels.add(label);
        }
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.evaluation.Evaluation#outputEvaluationResult(java.io.PrintStream)
     */
    public void outputEvaluationResult(PrintStream output)
    {
        output.print("### classes:\n### ");
        Util.print(this.labels,",",output);
        output.println();
    }

}
