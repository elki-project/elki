package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * TODO comment
 *
 * @author Arthur Zimek
 */
public abstract class AbstractClassifierEvaluation<O extends DatabaseObject, L extends ClassLabel<L>, C extends Classifier<O, L>> extends AbstractResult<O> implements Evaluation<O, C> {

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
        super(database);
        this.testset = testset;
        this.classifier = classifier;
    }

    /**
     * @param normalization normalization is unused
     */
    public final void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        PrintStream output;
        try {
            out.getParentFile().mkdirs();
            output = new PrintStream(new FileOutputStream(out));
        }
        catch (FileNotFoundException e) {
            //System.err.println("designated output file \"" + out.getAbsolutePath() + "\" cannot be created or is not writtable. Output is given to STDOUT.");
            warning("designated output file \"" + out.getAbsolutePath() + "\" cannot be created or is not writtable. Output is given to STDOUT.");
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        catch (Exception e) {
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        output(output, normalization, settings);
    }

    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        writeHeader(outStream, settings, null);
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
