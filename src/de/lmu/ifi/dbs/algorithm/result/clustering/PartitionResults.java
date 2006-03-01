package de.lmu.ifi.dbs.algorithm.result.clustering;


import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.Database;
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
public class PartitionResults<O extends DatabaseObject> extends AbstractResult<O> implements ClusteringResult<O>
{
    /**
     * A prefix for partition marks.
     */
    public static final String PARTITION_MARKER = "PartitionID";

    /**
     * Holds the results for the partitions.
     */
    private Map<Integer, ClusteringResult<O>> partitionResults;

    /**
     * A result for a partitioning algorithm providing a single result for a
     * single partition.
     * 
     * @param resultMap
     *            a map of partition IDs to results
     */
    public PartitionResults(Database<O> db, Map<Integer, ClusteringResult<O>> resultMap)
    {
        super(db);
        this.partitionResults = resultMap;
    }

    /**
     * @see Result#output(File, Normalization, List)
     */
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        for(Integer resultID : partitionResults.keySet())
        {
            Result<O> result = partitionResults.get(resultID);
            String marker = File.separator + PARTITION_MARKER + resultID;
            if(out == null)
            {
                System.out.println(marker);
                result.output(out, normalization, settings);
            }
            else
            {
                File markedOut = new File(out.getAbsolutePath() + marker);
                markedOut.getParentFile().mkdirs();
                result.output(markedOut, normalization, settings);
            }
        }
    }

    /**
     * Returns an iterator over the partition IDs.
     * 
     * @return an iterator over the partition IDs
     */
    public Iterator<Integer> partitionsIterator()
    {
        return partitionResults.keySet().iterator();
    }

    /**
     * Returns the result of the specified partition.
     * 
     * @param partitionID
     *            the ID of the partition
     * @return the result of the specified partition
     */
    public ClusteringResult<O> getResult(Integer partitionID)
    {
        return partitionResults.get(partitionID);
    }

    public <L extends ClassLabel<L>> Database<O> associate(Class<L> classLabel)
    {
        for(Integer partitionID : partitionResults.keySet())
        {
            getResult(partitionID).clustering(SimpleClassLabel.class);
        }
        // TODO Auto-generated method stub
        return null;
    }

    public <L extends ClassLabel<L>> Map<L, Database<O>> clustering(Class<L> classLabel)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    
}
