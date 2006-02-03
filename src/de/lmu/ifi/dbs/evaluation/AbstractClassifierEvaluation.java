package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractClassifierEvaluation<M extends MetricalObject, C extends Classifier<M>> extends AbstractResult<M> implements Evaluation<M, C>
{
    /**
     * Holds the used classifier.
     */
    private C classifier;
    
    private Database<M> testset;

    /**
     * 
     * @param database
     * @param classifier
     */
    public AbstractClassifierEvaluation(Database<M> database, Database<M> testset, C classifier)
    {
        super(database);
        this.testset = testset;
        this.classifier = classifier;
    }

    /**
     * 
     * @param normalization Normalization is unused.
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    public final void output(File out, Normalization<M> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        PrintStream output;
        try
        {
            out.getParentFile().mkdirs();
            output = new PrintStream(new FileOutputStream(out));
        }
        catch(FileNotFoundException e)
        {
            System.err.println("WARNING: designated output file \""+out.getAbsolutePath()+"\" cannot be created or is not writtable. Output is given to STDOUT.");
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        writeHeader(output,settings);
        output.print("Evaluating ");
        output.println(classifier.getClass().getName());
        output.println(classifier.getAttributeSettings());
        if(testset!=null)
        {
            output.println("used testset: ");
            output.print(" number of test instances: ");
            output.println(testset.size());
        }
        output.println("\nModel:");
        output.println(classifier.model());
        outputEvaluationResult(output);
    }

}
