package de.lmu.ifi.dbs.algorithm.result.clustering;


import static de.lmu.ifi.dbs.algorithm.result.clustering.PartitionResults.PARTITION_LABEL_PREFIX;
import static de.lmu.ifi.dbs.algorithm.result.clustering.PartitionResults.PARTITION_MARKER;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
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
    private Map<Integer, ClusteringResult<O>> partitionResults;
    
    /**
     * A partitionID that contains per definition noise - may remain null.
     */
    private Integer noise;

    /**
     * A result for a partitioning algorithm providing a single result for a
     * single partition.
     * 
     * @param db the database
     * @param resultMap
     *            a map of partition IDs to results
     * @param noise a partitionID that contains per definition noise - may remain null
     */
    public PartitionResults(Database<O> db, Map<Integer, ClusteringResult<O>> resultMap, Integer noise)
    {
        super(db);
        this.partitionResults = resultMap;
        this.noise = noise;
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
                PrintStream pout = new PrintStream(new FileOutputStream(FileDescriptor.out));
                pout.println(marker);
                result.output(pout, normalization, settings);
            }
            else
            {
                File markedOut = new File(out.getAbsolutePath() + marker);
                markedOut.getParentFile().mkdirs();
                result.output(markedOut, normalization, settings);
            }
        }
    }
    
    

    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        for(Integer resultID : partitionResults.keySet())
        {
            Result<O> result = partitionResults.get(resultID);
            String marker = File.separator + PARTITION_MARKER + resultID;
            outStream.println(marker);
            result.output(outStream, normalization, settings);

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

    /**
     * Returns a database containing only non-noise objects.
     * @param classLabel the most convenient {@link ClassLabel ClassLabel}
     * is the {@link HierarchicalClassLabel HierarchicalClassLabel}, that would have as top-level label
     * the partition id.
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult#associate(java.lang.Class)
     */
    public <L extends ClassLabel<L>> Database<O> associate(Class<L> classLabel)
    {        
        Map<Integer,List<Integer>> partitions = new HashMap<Integer,List<Integer>>();
        Integer zero = 0;
        partitions.put(zero, new ArrayList<Integer>());
        Database<O> database = null;
        try
        {
            database = this.db.partition(partitions).get(zero);
        }
        catch(UnableToComplyException e1)
        {
            e1.printStackTrace();
        }
        List<ObjectAndAssociations<O>> objectsAndAssociationsList = new ArrayList<ObjectAndAssociations<O>>();
        for(Integer partitionID : partitionResults.keySet())
        {
            if(noise == null || !partitionID.equals(noise))
            {
                Map<SimpleClassLabel,Database<O>> map = getResult(partitionID).clustering(SimpleClassLabel.class);
                for(SimpleClassLabel simpleLabel : map.keySet())
                {
                        L label = Util.instantiate(classLabel, classLabel.getName());
                        label.init(PARTITION_LABEL_PREFIX+partitionID+HierarchicalClassLabel.DEFAULT_SEPARATOR_STRING+simpleLabel.toString());
                        for(Iterator<Integer> ids = map.get(simpleLabel).iterator(); ids.hasNext();)
                        {
                            Integer id = ids.next();
                            Map<AssociationID,Object> association = new HashMap<AssociationID,Object>();
                            association.put(AssociationID.CLASS, label);
                            association.putAll(this.db.getAssociations(id));
                            ObjectAndAssociations<O> o = new ObjectAndAssociations<O>(this.db.get(id),association);
                            objectsAndAssociationsList.add(o);
                        }
                     
                }
            }
        }
        try
        {
            database.insert(objectsAndAssociationsList);
        }
        catch(UnableToComplyException e)
        {
            e.printStackTrace();
        }
        return database;
    }

    /**
     * Returns a mapping to databases containing only non-noise objects.
     * @param classLabel the most convenient {@link ClassLabel ClassLabel}
     * is the {@link HierarchicalClassLabel HierarchicalClassLabel}, that would have as top-level label
     * the partition id.
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult#clustering(java.lang.Class)
     */
    public <L extends ClassLabel<L>> Map<L, Database<O>> clustering(Class<L> classLabel)
    {
        Map<L, Database<O>> result = new HashMap<L, Database<O>>();
        for(Integer partitionID : partitionResults.keySet())
        {
            if(noise == null || !partitionID.equals(noise))
            {
                Map<SimpleClassLabel,Database<O>> map = getResult(partitionID).clustering(SimpleClassLabel.class);
                for(SimpleClassLabel simpleLabel : map.keySet())
                {
                    //L label = classLabel.newInstance();
                    L label = Util.instantiate(classLabel,classLabel.getName());
                    label.init(PARTITION_LABEL_PREFIX+partitionID+HierarchicalClassLabel.DEFAULT_SEPARATOR_STRING+simpleLabel.toString());
                    result.put(label, map.get(simpleLabel));
                }
            }
        }
        return result;
    }

    public <L extends ClassLabel<L>> void appendModel(L clusterID, Result<O> model)
    {
        String[] labels = HierarchicalClassLabel.DEFAULT_SEPARATOR.split(clusterID.toString());
        Integer partitionID = Integer.parseInt(labels[0].substring(PARTITION_LABEL_PREFIX.length()));
        L subclusterID = Util.instantiate((Class<L>) clusterID.getClass(), clusterID.getClass().getName());
        StringBuilder label = new StringBuilder();
        for(int i = 1; i < labels.length; i++)
        {
            label.append(labels[i]);
        }
        subclusterID.init(label.toString());
        partitionResults.get(partitionID).appendModel(subclusterID, model);
    }
    
    
}
