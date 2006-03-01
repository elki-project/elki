package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
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
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a result of a clustering-algorithm that computes several clusters.
 * The result is not to treat noise separately. All objects are assigned to
 * a certain cluster.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class Clusters<O extends DatabaseObject> extends AbstractResult<O> implements ClusteringResult<O>
{
    /**
     * Marker for a file name of a cluster.
     */
    public static final String CLUSTER_MARKER = "cluster";

    /**
     * An array of clusters, respectively, where each array provides the object
     * ids of its members
     */
    protected Integer[][] clusters;

    /**
     * Provides a result of a clustering-algorithm that computes several
     * clusters and remaining noise.
     * 
     * @param clusters
     *            an array of clusters where each array
     *            provides the object ids of its members
     * @param db
     *            the database containing the objects of clusters
     */
    public Clusters(Integer[][] clusters, Database<O> db)
    {
        super(db);
        this.clusters = clusters;
    }

    /**
     * @see Result#output(File, Normalization, List)
     */
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        for(int c = 0; c < this.clusters.length; c++)
        {
            String marker = CLUSTER_MARKER + format(c + 1, clusters.length - 1);
            PrintStream markedOut;
            try
            {
                File markedFile = new File(out.getAbsolutePath() + File.separator + marker);
                markedFile.getParentFile().mkdirs();
                markedOut = new PrintStream(new FileOutputStream(markedFile));
            }
            catch(Exception e)
            {
                markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
                markedOut.println(marker + ":");
            }
            try
            {
                write(c, markedOut, normalization, settings);
            }
            catch(NonNumericFeaturesException e)
            {
                throw new UnableToComplyException(e);
            }
            markedOut.flush();
        }

    }

    /**
     * Returns an integer-string for the given input, that has as many leading
     * zeros as to match the length of the specified maximum.
     * 
     * @param input
     *            an integer to be formatted
     * @param maximum
     *            the maximum to adapt the format to
     * @return an integer-string for the given input, that has as many leading
     *         zeros as to match the length of the specified maximum
     */
    protected String format(int input, int maximum)
    {
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        formatter.setMinimumIntegerDigits(Integer.toString(maximum).length());
        return formatter.format(input);
    }

    /**
     * Writes a cluster denoted by its cluster number to the designated print
     * stream.
     * 
     * @param clusterIndex
     *            the number of the cluster to be written
     * @param out
     *            the print stream where to write
     * @param normalization
     *            a Normalization to restore original values for output - may
     *            remain null
     * @param settings
     *            the settings to be written into the header
     * @throws NonNumericFeaturesException
     *             if feature vector is not compatible with values initialized
     *             during normalization
     */
    private void write(int clusterIndex, PrintStream out, Normalization<O> normalization, List<AttributeSettings> settings) throws NonNumericFeaturesException
    {
        writeHeader(out, settings);
        for(int i = 0; i < clusters[clusterIndex].length; i++)
        {
            O mo = db.get(clusters[clusterIndex][i]);
            if(normalization != null)
            {
                mo = normalization.restore(mo);
            }
            out.println(mo.toString() + SEPARATOR + db.getAssociation(AssociationID.LABEL, clusters[clusterIndex][i]));
        }
    }


    /**
     * Returns the array of clusters where each array
     * provides the object ids of its members.
     * 
     * @return the array of clusters and noise
     */
    public Integer[][] getClustersArray()
    {
        return clusters;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult#associate(java.lang.Class)
     */
    public <L extends ClassLabel<L>> Database<O> associate(Class<L> classLabel)
    {
        for(int clusterID = 0; clusterID < clusters.length; clusterID++)
        {
            try
            {
                L label = classLabel.newInstance();
                label.init("C"+Integer.toString(clusterID+1));
                for(int idIndex = 0; idIndex < clusters[clusterID].length; idIndex++)
                {
                    this.db.associate(AssociationID.CLASS, clusters[clusterID][idIndex], label);
                }
            }
            catch(InstantiationException e)
            {
                e.printStackTrace();
            }
            catch(IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        return this.db;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult#clustering(java.lang.Class)
     */
    public <L extends ClassLabel<L>> Map<L, Database<O>> clustering(Class<L> classLabel)
    {   
        Map<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
        for(int clusterID = 0; clusterID < clusters.length; clusterID++)
        {
            List<Integer> ids = Arrays.asList(clusters[clusterID]);
            partitions.put(clusterID, ids);
        }   
        Map<L, Database<O>> map = new HashMap<L, Database<O>>();
        try
        {
            Map<Integer,Database<O>> partitionMap = this.db.partition(partitions);
            
            for(Integer partitionID : partitionMap.keySet())
            {
                L label = classLabel.newInstance();
                label.init("C"+Integer.toString(partitionID+1));
                map.put(label, partitionMap.get(partitionID));
            }
        }
        catch(InstantiationException e)
        {
            e.printStackTrace();
        }
        catch(IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch(UnableToComplyException e)
        {
            e.printStackTrace();
        }
        
        return map;
    }

    
}
