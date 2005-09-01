package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.io.File;
import java.util.Map;
import java.util.Iterator;

/**
 * A result for a partitioning algorithm providing a single result for a single
 * partition.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PartitionResults implements Result {
  public static final String PARTITION_MARKER = "PartitionID";

  /**
   * Holds the results for the partitions.
   */
  private Map<Integer, Result> partitionResults;

  /**
   * A result for a partitioning algorithm providing a single result for a
   * single partition.
   *
   * @param resultMap a map of partition IDs to results
   */
  public PartitionResults(Map<Integer, Result> resultMap) {
    this.partitionResults = resultMap;
  }

  /**
   * @see Result#output(File, Normalization)
   */
  public void output(File out, Normalization normalization) throws UnableToComplyException {
    for (Integer resultID : partitionResults.keySet()) {
      Result result = partitionResults.get(resultID);
      String marker = File.separator + PARTITION_MARKER + resultID;
      if (out == null) {
        System.out.println(marker);
        result.output(out, normalization);
      }
      else {
        File markedOut = new File(out.getAbsolutePath() + marker);
        markedOut.getParentFile().mkdirs();
        result.output(markedOut, normalization);
      }
    }
  }

  /**
   * Returns an iterator over the partition IDs.
   * @return an iterator over the partition IDs
   */
  public Iterator<Integer> partitionIterator() {
    return partitionResults.keySet().iterator();
  }

  /**
   * Returns the result of the specified partition.
   * @param partitionID the ID of the partition
   * @return the result of the specified partition
   */
  public Result getResult(Integer partitionID) {
    return partitionResults.get(partitionID);
  }
}
