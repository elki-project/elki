package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.ClustersPlusNoise;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * DBSCAN provides the DBSCAN algorithm.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DBSCAN<T extends MetricalObject> extends DistanceBasedAlgorithm<T>
{
    /**
     * Parameter for epsilon.
     */
    public static final String EPSILON_P = "epsilon";

    /**
     * Description for parameter epsilon.
     */
    public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the specified distance function";

    /**
     * Parameter minimum points.
     */
    public static final String MINPTS_P = "minpts";

    /**
     * Description for parameter minimum points.
     */
    public static final String MINPTS_D = "<int>minpts";

    /**
     * Epsilon.
     */
    protected String epsilon;

    /**
     * Minimum points.
     */
    protected int minpts;

    /**
     * Holds a list of clusters found.
     */
    protected List<List<Integer>> resultList;

    /**
     * Provides the result of the algorithm.
     */
    protected ClustersPlusNoise<T> result;

    /**
     * Holds a set of noise.
     */
    protected Set<Integer> noise;

    /**
     * Holds a set of processed ids.
     */
    protected Set<Integer> processedIDs;

    /**
     * Sets epsilon and minimum points to the optionhandler additionally to the
     * parameters provided by super-classes. Since DBSCAN is a non-abstract
     * class, finally optionHandler is initialized.
     */
    public DBSCAN()
    {
        super();
        parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
        parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
        optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
    }

    /**
     * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public void runInTime(Database<T> database) throws IllegalStateException
    {
        if(isVerbose())
        {
            System.out.println();
        }
        try
        {
            Progress progress = new Progress(database.size());
            resultList = new ArrayList<List<Integer>>();
            noise = new HashSet<Integer>();
            processedIDs = new HashSet<Integer>(database.size());
            getDistanceFunction().setDatabase(database, isVerbose());

            if(database.size() >= minpts)
            {
                for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
                {
                    Integer id = iter.next();
                    if(!processedIDs.contains(id))
                    {
                        expandCluster(database, id, progress);
                        if(processedIDs.size() == database.size() && noise.size() == 0)
                        {
                            break;
                        }
                    }
                    if(isVerbose())
                    {
                        progress.setProcessed(processedIDs.size());
                        System.out.println(status(progress, resultList.size()));
                    }
                }
            }
            else
            {
                for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
                {
                    Integer id = iter.next();
                    noise.add(id);
                    if(isVerbose())
                    {
                        progress.setProcessed(processedIDs.size());
                        System.out.println(status(progress, resultList.size()));
                    }
                }
            }

            if(isVerbose())
            {
                progress.setProcessed(processedIDs.size());
                System.out.println(status(progress, resultList.size()));
            }

            Integer[][] resultArray = new Integer[resultList.size() + 1][];
            int i = 0;
            for(Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++)
            {
                resultArray[i] = resultListIter.next().toArray(new Integer[0]);
            }

            resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
            result = new ClustersPlusNoise<T>(resultArray, database);
            if(isVerbose())
            {
                progress.setProcessed(processedIDs.size());
                System.out.println(status(progress, resultList.size()));
                System.out.println();
            }
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e);
        }

    }

    /**
     * DBSCAN-function expandCluster. <p/> Border-Objects become members of the
     * first possible cluster.
     * 
     * @param database
     *            the database on which the algorithm is run
     * @param startObjectID
     *            potential seed of a new potential cluster
     */
    protected void expandCluster(Database<T> database, Integer startObjectID, Progress progress)
    {
        List<QueryResult> seeds = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());

        // startObject is no core-object
        if(seeds.size() < minpts)
        {
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
            if(isVerbose())
            {
                progress.setProcessed(processedIDs.size());
                System.out.print(status(progress, resultList.size()));
            }
            return;
        }

        List<Integer> currentCluster = new ArrayList<Integer>();
        for(QueryResult seed : seeds)
        {
            Integer nextID = seed.getID();
            if(!processedIDs.contains(nextID))
            {
                currentCluster.add(nextID);
                processedIDs.add(nextID);
            }
            else if(noise.contains(nextID))
            {
                currentCluster.add(nextID);
                noise.remove(nextID);
            }
        }
        seeds.remove(0);

        while(seeds.size() > 0)
        {
            Integer o = seeds.remove(0).getID();
            List<QueryResult> neighborhood = database.rangeQuery(o, epsilon, getDistanceFunction());
            if(neighborhood.size() >= minpts)
            {
                for(QueryResult neighbor : neighborhood)
                {
                    Integer p = neighbor.getID();
                    boolean inNoise = noise.contains(p);
                    boolean unclassified = !processedIDs.contains(p);
                    if(inNoise || unclassified)
                    {
                        if(unclassified)
                        {
                            seeds.add(neighbor);
                        }
                        currentCluster.add(p);
                        processedIDs.add(p);
                        if(inNoise)
                        {
                            noise.remove(p);
                        }
                    }
                }
            }

            if(isVerbose())
            {
                progress.setProcessed(processedIDs.size());
                int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
                System.out.print(status(progress, numClusters));
            }

            if(processedIDs.size() == database.size() && noise.size() == 0)
            {
                break;
            }
        }
        if(currentCluster.size() >= minpts)
        {
            resultList.add(currentCluster);
        }
        else
        {
            for(Integer id : currentCluster)
            {
                noise.add(id);
            }
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
        }
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("DBSCAN", "Density-Based Clustering of Applications with Noise", "Algorithm to find density-connected sets in a database based on the parameters " + "minimumPoints and epsilon (specifying a volume). " + "These two parameters determine a density threshold for clustering.", "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: " + "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. " + "In: Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), " + "Portland, OR, 1996.");
    }

    /**
     * Sets the parameters epsilon and minpts additionally to the parameters set
     * by the super-class' method. Both epsilon and minpts are required
     * parameters.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            getDistanceFunction().valueOf(optionHandler.getOptionValue(EPSILON_P));
            epsilon = optionHandler.getOptionValue(EPSILON_P);
            minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result<T> getResult()
    {
        return result;
    }

    /**
     * Returns the parameter setting of this algorithm.
     * 
     * @return the parameter setting of this algorithm
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> result = new ArrayList<AttributeSettings>();

        AttributeSettings attributeSettings = new AttributeSettings(this);
        attributeSettings.addSetting(DISTANCE_FUNCTION_P, getDistanceFunction().getClass().getSimpleName());
        attributeSettings.addSetting(EPSILON_P, epsilon);
        attributeSettings.addSetting(MINPTS_P, Integer.toString(minpts));

        result.add(attributeSettings);
        return result;
    }

    /**
     * Provides a status report line with leading carriage return.
     * 
     * @param progress
     *            the progress status
     * @param clusters
     *            current number of clusters
     * @return a status report line with leading carriage return
     */
    public static String status(Progress progress, int clusters)
    {
        StringBuffer status = new StringBuffer();
        status.append("\r");
        status.append(progress.toString());
        status.append(" Number of clusters: ");
        status.append(clusters);
        status.append(".                           ");
        return status.toString();
    }
}
