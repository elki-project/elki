package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A result for a partitioning algorithm providing a single result for a single
 * partition.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PartitionResults<T extends MetricalObject> implements Result<T> {
  public static final String PARTITION_MARKER = "PartitionID";

  /**
   * Holds the results for the partitions.
   */
  private Map<Integer, Result<T>> partitionResults;

  /**
   * A result for a partitioning algorithm providing a single result for a
   * single partition.
   *
   * @param resultMap a map of partition IDs to results
   */
  public PartitionResults(Map<Integer, Result<T>> resultMap) {
    this.partitionResults = resultMap;
  }

  /**
   * @see Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, 
   * java.util.List<de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings>)
   */
  public void output(File out, Normalization<T> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    for (Integer resultID : partitionResults.keySet()) {
      Result<T> result = partitionResults.get(resultID);
      String marker = File.separator + PARTITION_MARKER + resultID;
      if (out == null) {
        System.out.println(marker);
        result.output(out, normalization, null);
      }
      else {
        File markedOut = new File(out.getAbsolutePath() + marker);
        markedOut.getParentFile().mkdirs();
        result.output(markedOut, normalization, settings);
      }
    }
  }

  /**
   * Returns an iterator over the partition IDs.
   * @return an iterator over the partition IDs
   */
  public Iterator<Integer> partitionsIterator() {
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
