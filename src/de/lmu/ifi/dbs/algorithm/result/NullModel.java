package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A model describing the database and the available class labels.
 * As an empty model this model may be suitable for lazy learners.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class NullModel<M extends MetricalObject> extends AbstractResult<M>
{
    /**
     * The labels available for classification.
     */
    protected List<String> labels;

    /**
     * Provides a new NullModel for the given database and labels.
     * 
     * @param db the database where the NullModel is bsaed on
     * @param labels the labels available for classification
     */
    public NullModel(Database<M> db, String[] labels)
    {
        super(db);
        this.labels = new ArrayList<String>(labels.length);
        for(String label : labels)
        {
            this.labels.add(label);
        }
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
        output.print("### classes:\n### ");
        Util.print(this.labels,",",output);
        output.println();
    }

}
