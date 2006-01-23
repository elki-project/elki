package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PriorProbability<M extends MetricalObject> extends NullModel<M>
{
    protected double[] priorProbability;

    /**
     * @param db
     * @param labels
     * @param priorProbability
     */
    public PriorProbability(Database<M> db, ClassLabel[] labels, double[] priorProbability)
    {
        super(db, labels);
        this.priorProbability = new double[priorProbability.length];
        System.arraycopy(priorProbability,0,this.priorProbability,0,priorProbability.length);
    }
    
    /**
     * Writes header informations and class labels to the designated output.
     * 
     * @see Result#output(File, Normalization, List)
     */
    public void output(File out, Normalization<M> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        PrintStream output;
        try
        {
            File markedFile = new File(out.getAbsolutePath());
            markedFile.getParentFile().mkdirs();
            output = new PrintStream(new FileOutputStream(markedFile));
        }
        catch(Exception e)
        {
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        try
        {
            writeHeader(output,settings);
        }
        catch(NonNumericFeaturesException e)
        {
            throw new UnableToComplyException(e);
        }
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
