package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KNNDistanceOrderResult<O extends MetricalObject, D extends Distance<D>> extends AbstractResult<O>
{
    private final List<D> knnDistances;

    /**
     * @param db
     */
    public KNNDistanceOrderResult(final Database<O> db, final List<D> knnDistances)
    {
        super(db);
        this.knnDistances = knnDistances;
    }

    /**
     * @see Result#output(java.io.File,
     *      de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    public void output(final File out, final Normalization<O> normalization, final List<AttributeSettings> settings) throws UnableToComplyException
    {
        PrintStream output;
        try
        {
            out.getParentFile().mkdirs();
            output = new PrintStream(new FileOutputStream(out));
        }
        catch(Exception e)
        {
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        super.writeHeader(output, settings);
        Util.print(knnDistances, System.getProperty("line.separator"), output);
        output.println();
    

    }

}
