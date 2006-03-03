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
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.*;

/**
 * Provides a result of a clustering-algorithm that computes several clusters
 * and remaining noise.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClustersPlusNoise<O extends DatabaseObject> extends AbstractResult<O> implements ClusteringResult<O>
{
    /**
     * Marker for a file name of a cluster.
     */
    public static final String CLUSTER_MARKER = "cluster";

    /**
     * Marker for a file name of noise.
     */
    public static final String NOISE_MARKER = "noise";

    public static final String CLUSTER_LABEL_PREFIX = "C";

    protected Map<Integer,Result<O>> clusterToModel;
    
    
    /**
     * An array of clusters and noise, respectively, where each array provides
     * the object ids of its members.
     * The array at the last position comprises the ids of noise objects.
     */
    Integer[][] clustersAndNoise;

    /**
     * Provides a result of a clustering-algorithm that computes several
     * clusters and remaining noise.
     * 
     * @param clustersAndNoise
     *            an array of clusters and noise, respectively, where each array
     *            provides the object ids of its members
     * @param db
     *            the database containing the objects of clusters
     */
    public ClustersPlusNoise(Integer[][] clustersAndNoise, Database<O> db)
    {
        super(db);
        this.clustersAndNoise = clustersAndNoise;
        this.db = db;
        clusterToModel = new HashMap<Integer,Result<O>>();
    }

    /**
     * @see Result#output(File, Normalization, List)
     */
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        for(int c = 0; c < this.clustersAndNoise.length; c++)
        {
            String marker;
            if(c < clustersAndNoise.length - 1)
            {
                marker = CLUSTER_MARKER + format(c + 1, clustersAndNoise.length - 1);
            }
            else
            {
                marker = NOISE_MARKER;
            }
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

    
    
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        for(int c = 0; c < this.clustersAndNoise.length; c++)
        {
            String marker;
            if(c < clustersAndNoise.length - 1)
            {
                marker = CLUSTER_MARKER + format(c + 1, clustersAndNoise.length - 1);
            }
            else
            {
                marker = NOISE_MARKER;
            }
            PrintStream markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
            markedOut.println(marker + ":");
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
        Result<O> model = clusterToModel.get(clusterIndex); 
        if(model != null)
        {
            try
            {
                model.output(out, normalization, settings);
            }
            catch(UnableToComplyException e)
            {
                e.printStackTrace();
            }
        }
        
        for(int i = 0; i < clustersAndNoise[clusterIndex].length; i++)
        {
            O mo = db.get(clustersAndNoise[clusterIndex][i]);
            if(normalization != null)
            {
                mo = normalization.restore(mo);
            }
            out.println(mo.toString() + SEPARATOR + db.getAssociation(AssociationID.LABEL, clustersAndNoise[clusterIndex][i]));
        }
    }



    /**
     * Returns the array of clusters and noise, respectively, where each array
     * provides the object ids of its members.
     * 
     * @return the array of clusters and noise
     */
    public Integer[][] getClusterAndNoiseArray()
    {
        return clustersAndNoise;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult#associate(java.lang.Class)
     */
    public <L extends ClassLabel<L>> Database<O> associate(Class<L> classLabel)
    {
        List<Integer> nonNoiseObjects = new ArrayList<Integer>();
        for(int clusterID = 0; clusterID < clustersAndNoise.length - 1; clusterID++)
        {
            nonNoiseObjects.addAll(Arrays.asList(clustersAndNoise[clusterID]));
        }
        Integer clusters = 1;
        Map<Integer,List<Integer>> partitions = new HashMap<Integer,List<Integer>>();
        partitions.put(clusters, nonNoiseObjects);
        Database<O> clusterDB = null;
        try
        {
            clusterDB = this.db.partition(partitions).get(clusters);
        
            for(int clusterID = 0; clusterID < clustersAndNoise.length - 1; clusterID++)
            {
                L label = Util.instantiate(classLabel,classLabel.getName());
                label.init("C"+Integer.toString(clusterID+1));
                for(int idIndex = 0; idIndex < clustersAndNoise[clusterID].length; idIndex++)
                {
                    clusterDB.associate(AssociationID.CLASS, clustersAndNoise[clusterID][idIndex], label);
                }
            
                
            }
        }
        catch(UnableToComplyException e1)
        {
            e1.printStackTrace();
        }
        return clusterDB;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult#clustering(java.lang.Class)
     */
    public <L extends ClassLabel<L>> Map<L, Database<O>> clustering(Class<L> classLabel)
    {
        Map<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
        for(int clusterID = 0; clusterID < clustersAndNoise.length-1; clusterID++)
        {
            List<Integer> ids = Arrays.asList(clustersAndNoise[clusterID]);
            partitions.put(clusterID, ids);
        }   
        Map<L, Database<O>> map = new HashMap<L, Database<O>>();
        try
        {
            Map<Integer,Database<O>> partitionMap = this.db.partition(partitions);
            
            for(Integer partitionID : partitionMap.keySet())
            {
                L label = Util.instantiate(classLabel,classLabel.getName());
                label.init("C"+Integer.toString(partitionID+1));
                map.put(label, partitionMap.get(partitionID));
            }
        }
        catch(UnableToComplyException e)
        {
            e.printStackTrace();
        }
        
        return map;
    }

    public <L extends ClassLabel<L>> void appendModel(L clusterID, Result<O> model)
    {
        clusterToModel.put(classLabelToClusterID(clusterID), model);
    }
    
    protected <L extends ClassLabel<L>> Integer classLabelToClusterID(L classLabel)
    {
        return Integer.parseInt(classLabel.toString().substring(CLUSTER_LABEL_PREFIX.length()));
    }


    protected String canonicalClusterLabel(int clusterID)
    {
        return CLUSTER_LABEL_PREFIX+Integer.toString(clusterID+1);
    }
    
}
