package de.lmu.ifi.dbs.algorithm.result.outlier;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.IDDoublePair;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides the result of the SOD algorithm.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 */

public class SODResult<O extends RealVector<O, Double>> extends AbstractResult<O> {

    /**
     * Marker for a file name containing lofs.
     */
    public static final String SOD_MARKER = "sod";

    /**
     * A new SODResult set for a database.
     * <p/>
     * The database needs to contain associations for the computed SODs with
     * <code>AssociationID</code>
     * {@link de.lmu.ifi.dbs.database.AssociationID#SOD_MODEL SOD_MODEL}.
     *
     * @param db the database containing the SODs as association
     */
    public SODResult(Database<O> db) {
        super(db);
        this.db = db;
    }

    /**
     * @see AbstractResult#output(java.io.File,
     *de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
     */
    @Override
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        PrintStream outStream;
        try {
            File sodFile = new File(out.getAbsolutePath() + File.separator + SOD_MARKER + FILE_EXTENSION);
            sodFile.getParentFile().mkdirs();
            PrintStream sodOut = new PrintStream(new FileOutputStream(sodFile));
            outputSOD(sodOut, normalization, settings);
            sodOut.flush();

        }
        catch (Exception e) {
            outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
            output(outStream, normalization, settings);
        }
    }

    /**
     * @see AbstractResult#output(java.io.PrintStream,
     *de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

        outputSOD(outStream, normalization, settings);
        outStream.flush();
    }

    /**
     * Writes the lofs to output.
     *
     * @param outStream     the stream to write to
     * @param normalization Normalization to restore original values according to,
     *                      if this action is supported - may remain null.
     * @param settings      the settings to be written into the header, if this
     *                      parameter is <code>null</code>, no header will be written
     * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
     *          if an exception during normalization occurs
     */
    private void outputSOD(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

        writeHeader(outStream, settings, null);

        try {
            // sort ascendingly according to sod
            List<IDDoublePair> list = new LinkedList<IDDoublePair>();
            for (Iterator<Integer> it = db.iterator(); it.hasNext();) {
                Integer id = it.next();
                // noinspection unchecked
                SODModel<O> sodModel = db.getAssociation(AssociationID.SOD_MODEL, id);
                list.add(new IDDoublePair(id, sodModel.getSod()));
            }
            Collections.sort(list);
            // write sods
            for (IDDoublePair iddoublepair : list) {
                Integer id = iddoublepair.getID();

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

                outStream.println("SOD=" + iddoublepair.getValue());
                // noinspection unchecked
                SODModel<O> sodModel = db.getAssociation(AssociationID.SOD_MODEL, id);
                sodModel.output(outStream, normalization, settings);

            }
        }
        catch (NonNumericFeaturesException e) {
            throw new UnableToComplyException(e);
        }

        outStream.flush();
    }

}
