package de.lmu.ifi.dbs.evaluation;

import java.io.*;
import java.util.List;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

/**
 * TODO comment
 * 
 * @author Arthur Zimek 
 */
public abstract class AbstractClassifierEvaluation<O extends DatabaseObject, L extends ClassLabel<L>, C extends Classifier<O,L>> extends AbstractResult<O>  implements Evaluation<O, C>{
    
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
     */
    public AbstractClassifierEvaluation(Database<O> database, Database<O> testset, C classifier)
    {
        super(database);
        this.testset = testset;
        this.classifier = classifier;
    }

    /**
     * @param normalization
     *            Normalization is unused.
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File,
     *      de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    public final void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        PrintStream output;
        try
        {
            out.getParentFile().mkdirs();
            output = new PrintStream(new FileOutputStream(out));
        }
        catch(FileNotFoundException e)
        {
            //System.err.println("designated output file \"" + out.getAbsolutePath() + "\" cannot be created or is not writtable. Output is given to STDOUT.");
        	warning("designated output file \"" + out.getAbsolutePath() + "\" cannot be created or is not writtable. Output is given to STDOUT.");
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        catch(Exception e)
        {
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        output(output, normalization, settings);
    }

    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        writeHeader(outStream, settings, null);
        outStream.print("Evaluating ");
        outStream.println(classifier.getClass().getName());
        outStream.println(classifier.getAttributeSettings());
        if(testset != null)
        {
            outStream.println("used testset: ");
            outStream.print(" number of test instances: ");
            outStream.println(testset.size());
        }
        outStream.println("\nModel:");
        outStream.println(classifier.model());
        outputEvaluationResult(outStream);

    }

}
