package de.lmu.ifi.dbs.algorithm.result;


import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * Provides the result of the LOF algorithm.
 *
 * @author Arthur Zimek
 */

public class GeneralizedLOFResult<O extends DatabaseObject> extends AbstractResult<O>
{

    /**
     * Marker for a file name containing lofs.
     */
    public static final String LOF_MARKER = "lof";

    /**
     * A new LOFResult set for a database.
     * 
     * The database needs to contain associations for the computed lofs
     * with <code>AssociationID</code> {@link de.lmu.ifi.dbs.database.AssociationID#LOF LOF}.
     *
     * @param db       the database containing the LOFs as association
     */
    public GeneralizedLOFResult(Database<O> db)
    {
        super(db);
        this.db = db;
    }

    /**
     * @see AbstractResult#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        PrintStream outStream;
        try
        {
            File lofFile = new File(out.getAbsolutePath() + File.separator + LOF_MARKER + FILE_EXTENSION);
            lofFile.getParentFile().mkdirs();
            PrintStream lofOut = new PrintStream(new FileOutputStream(lofFile));
            outputLOF(lofOut, normalization, settings);
            lofOut.flush();

        }
        catch(Exception e)
        {
            outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
            output(outStream, normalization, settings);
        }
    }

    /**
     * @see AbstractResult#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {

        outputLOF(outStream, normalization, settings);
        outStream.flush();
    }

    /**
     * Writes the lofs to output.
     *
     * @param outStream     the stream to write to
     * @param normalization Normalization to restore original values according to, if this action is supported
     *                      - may remain null.
     * @param settings      the settings to be written into the header, if this parameter is <code>null</code>,
     *                      no header will be written
     */
    private void outputLOF(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {

        writeHeader(outStream, settings, null);

        try
        {
            

            // write lofs
            for(Iterator<Integer> it = db.iterator(); it.hasNext();)
            {
                Integer id = it.next();

                outStream.print(id);
                outStream.print(" ");

                O object = db.get(id);
                if(normalization != null)
                {
                    O restored = normalization.restore(object);
                    outStream.print(restored.toString());
                }
                else
                {
                    outStream.print(object.toString());
                }
                outStream.print(" ");

                String label = (String) db.getAssociation(AssociationID.LABEL, id);
                if(label != null)
                {
                    outStream.print(label);
                    outStream.print(" ");
                }

                ClassLabel classLabel = (ClassLabel) db.getAssociation(AssociationID.CLASS, id);
                if(classLabel != null)
                {
                    outStream.print(classLabel);
                    outStream.print(" ");
                }

                outStream.println(db.getAssociation(AssociationID.LOF, id));
            }
        }
        catch(NonNumericFeaturesException e)
        {
            throw new UnableToComplyException(e);
        }

        outStream.flush();
    }

}
