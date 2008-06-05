package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * A result for a partitioning algorithm providing a single result for a single
 * partition.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 * @param <O> the type of DatabaseObjects handled by this Result
 */
public class PartitionResults<O extends DatabaseObject> extends AbstractResult<O> {
    /**
     * A prefix for the partition-based class label.
     */
    public static final String PARTITION_LABEL_PREFIX = "P";

    /**
     * A prefix for partition marks.
     */
    public static final String PARTITION_MARKER = "PartitionID";

    /**
     * Holds the results for the partitions.
     */
    private Map<Integer, Result<O>> partitionResults;

    /**
     * A result for a partitioning algorithm providing a single result for a
     * single partition.
     *
     * @param db        the database
     * @param resultMap a map of partition IDs to results
     */
    public PartitionResults(Database<O> db, Map<Integer, Result<O>> resultMap) {
        super(db);
        this.partitionResults = resultMap;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File,
     *de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
     */
    @Override
    public void output(File out, Normalization<O> normalization,
                       List<AttributeSettings> settings) throws UnableToComplyException {
        for (Integer resultID : partitionResults.keySet()) {
            Result<O> result = partitionResults.get(resultID);
            String marker = File.separator + PartitionResults.PARTITION_MARKER
                + resultID + FILE_EXTENSION;
            if (out == null) {
                PrintStream pout = new PrintStream(new FileOutputStream(
                    FileDescriptor.out));
                pout.println(marker);
                result.output(pout, normalization, settings);
            }
            else {
                File markedOut = new File(out.getAbsolutePath() + marker);
                markedOut.getParentFile().mkdirs();
                result.output(markedOut, normalization, settings);
            }
        }
    }

    /**
     * @see Result#output(java.io.PrintStream,
     *de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization<O> normalization,
                       List<AttributeSettings> settings) throws UnableToComplyException {
        for (Integer resultID : partitionResults.keySet()) {
            Result<O> result = partitionResults.get(resultID);
            String marker = File.separator + PartitionResults.PARTITION_MARKER
                + resultID;
            outStream.println(marker);
            result.output(outStream, normalization, settings);
        }
    }
}
