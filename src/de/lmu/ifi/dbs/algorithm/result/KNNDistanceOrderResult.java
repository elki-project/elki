package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
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
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 * @param <D> the type of Distance used by this Result
 * todo arthur comment
 */
public class KNNDistanceOrderResult<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractResult<O> {

    private final List<D> knnDistances;

    /**
     * @param db
     * @param knnDistances
     */
    public KNNDistanceOrderResult(final Database<O> db,
                                  final List<D> knnDistances) {
        super(db);
        this.knnDistances = knnDistances;
    }

    /**
     * @see Result#output(java.io.File,
     *de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
     */
    public void output(final File out, final Normalization<O> normalization,
                       final List<AttributeSettings> settings)
        throws UnableToComplyException {
        PrintStream output;
        try {
            out.getParentFile().mkdirs();
            output = new PrintStream(new FileOutputStream(out));
        }
        catch (Exception e) {
            output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        output(output, normalization, settings);

    }

    public void output(PrintStream outStream, Normalization<O> normalization,
                       List<AttributeSettings> settings) throws UnableToComplyException {
        writeHeader(outStream, settings, null);
        Util.print(knnDistances, System.getProperty("line.separator"),
            outStream);
        outStream.println();

    }

}
