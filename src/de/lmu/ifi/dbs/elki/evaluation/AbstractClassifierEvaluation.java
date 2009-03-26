package de.lmu.ifi.dbs.elki.evaluation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

/**
 * TODO comment
 *
 * @author Arthur Zimek
 */
public abstract class AbstractClassifierEvaluation<O extends DatabaseObject, L extends ClassLabel, C extends Classifier<O, L, Result>> implements EvaluationResult<O, C> {
    /**
     * The referenced main database.
     */
    //TODO: used?
    protected Database<O> database;
  
    /**
     * Holds the used classifier.
     */
    private C classifier;

    /**
     * Holds the test set.
     */
    private Database<O> testset;

    /**
     * @param database
     * @param classifier
     * @param testset
     */
    public AbstractClassifierEvaluation(Database<O> database, Database<O> testset, C classifier) {
        this.database = database;
        this.testset = testset;
        this.classifier = classifier;
    }

    /**
     * low-level output of results
     * 
     * @param outStream output stream
     * @param normalization Normalization
     * @param settings Settings
     * @throws UnableToComplyException
     * @throws IOException
     */
    // TODO: Remove/Rewrite
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException, IOException {
        //writeHeader(outStream, settings, null);
        outStream.print("Evaluating ");
        outStream.println(classifier.getClass().getName());
        outStream.println(classifier.getAttributeSettings());
        if (testset != null) {
            outStream.println("used testset: ");
            outStream.print(" number of test instances: ");
            outStream.println(testset.size());
        }
        outStream.println("\nModel:");
        outStream.println(classifier.model());
        outputEvaluationResult(outStream);

    }

}
