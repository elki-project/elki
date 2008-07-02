package de.lmu.ifi.dbs.elki.algorithm.result.outlier;


import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.IDDoublePair;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides the result of the LOF algorithm.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 */

public class GeneralizedLOFResult<O extends DatabaseObject> extends AbstractResult<O> {

    /**
     * Marker for a file name containing lofs.
     */
    public static final String LOF_MARKER = "lof";

    /**
     * A new LOFResult set for a database.
     * <p/>
     * The database needs to contain associations for the computed lofs
     * with <code>AssociationID</code> {@link de.lmu.ifi.dbs.elki.database.AssociationID#LOF LOF}.
     *
     * @param db the database containing the LOFs as association
     */
    public GeneralizedLOFResult(Database<O> db) {
        super(db);
        this.db = db;
    }

    /**
     * @see AbstractResult#output(java.io.File,de.lmu.ifi.dbs.elki.normalization.Normalization,java.util.List)
     */
    @Override
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        PrintStream outStream;
        try {
            File lofFile = new File(out.getAbsolutePath() + File.separator + LOF_MARKER + FILE_EXTENSION);
            lofFile.getParentFile().mkdirs();
            PrintStream lofOut = new PrintStream(new FileOutputStream(lofFile));
            outputLOF(lofOut, normalization, settings);
            lofOut.flush();

        }
        catch (Exception e) {
            outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
            output(outStream, normalization, settings);
        }
    }

    /**
     * @see AbstractResult#output(java.io.PrintStream,de.lmu.ifi.dbs.elki.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

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
     * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException if an eror during normalization occurs
     */
    private void outputLOF(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

        writeHeader(outStream, settings, null);

        try {
            List<IDDoublePair> lofs = new ArrayList<IDDoublePair>(db.size());
            for (Iterator<Integer> it = db.iterator(); it.hasNext();) {
                Integer id = it.next();
                lofs.add(new IDDoublePair(id, db.getAssociation(AssociationID.LOF, id)));
            }
            Collections.sort(lofs);

            // write lofs
            for (IDDoublePair pair : lofs) {
                Integer id = pair.getID();

                outStream.print("ID=");
                outStream.print(id);
                outStream.print(" ");

                O object = db.get(id);
                if (normalization != null) {
                    O restored = normalization.restore(object);
                    outStream.print(restored.toString());
                }
                else {
                    outStream.print(object.toString());
                }
                outStream.print(" ");

                String label = db.getAssociation(AssociationID.LABEL, id);
                if (label != null) {
                    outStream.print(label);
                    outStream.print(" ");
                }

                ClassLabel<?> classLabel = db.getAssociation(AssociationID.CLASS, id);
                if (classLabel != null) {
                    outStream.print(classLabel);
                    outStream.print(" ");
                }

                outStream.print("LOF=");
                outStream.println(pair.getValue());
            }
        }
        catch (NonNumericFeaturesException e) {
            throw new UnableToComplyException(e);
        }

        outStream.flush();
    }

}
