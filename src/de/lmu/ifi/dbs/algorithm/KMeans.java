package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Clusters;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides the k-means algorithm.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KMeans<T extends FeatureVector> extends DistanceBasedAlgorithm<T>
{
    /**
     * Parameter k.
     */
    public static final String K_P = "k";
    
    /**
     * Description for parameter k.
     */
    public static final String K_D = "<int>k - the number of clusters to find (positive integer)";

    /**
     * Keeps k - the number of clusters to find.
     */
    private int k;
    
    /**
     * Keeps the result.
     */
    private Clusters result;
    
    /**
     * Provides the k-means algorithm.
     */
    public KMeans()
    {
        super();
        parameterToDescription.put(K_P+OptionHandler.EXPECTS_VALUE,K_D);
        optionHandler = new OptionHandler(parameterToDescription,KMeans.class.getName());
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        // TODO reference
        return new Description("K-Means", "K-Means", "finds a partitioning into k clusters", "...");
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result getResult()
    {
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public void run(Database<T> database) throws IllegalStateException
    {
        if(database.size() > 0)
        {
            T randomBase = database.get(database.iterator().next()); 
            List<T> means = new ArrayList<T>();
            List<List<Integer>> oldClusters = new ArrayList<List<Integer>>();
            List<List<Integer>> newClusters = new ArrayList<List<Integer>>();
            for(int i = 0; i < k; i++)
            {
                means.add((T) randomBase.randomInstance());
            }
            oldClusters = sort(means, database);
            boolean changed = true;
            while(changed)
            {
                means = means(oldClusters, database);
                newClusters = sort(means, database);
                changed = !oldClusters.equals(newClusters);
            }
            Integer[][] clusters = new Integer[newClusters.size()][];
            for(int i = 0; i < newClusters.size(); i++)
            {
                List<Integer> cluster = newClusters.get(i);
                clusters[i] = cluster.toArray(new Integer[cluster.size()]);
            }
            result = new Clusters<T>(clusters,database);
        }
        else
        {
            result = new Clusters<T>(new Integer[0][0], database);
        }
    }
    
    /**
     * Returns the mean vectors of the given clusters in the given database.
     * 
     * 
     * @param clusters the clusters to compute the means
     * @param database the database containing the vectors
     * @return the mean vectors of the given clusters in the given database
     */
    protected List<T> means(List<List<Integer>> clusters, Database<T> database)
    {
        List<T> means = new ArrayList<T>();
        for(List<Integer> list : clusters)
        {
            T mean = null;
            for(Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();)
            {
                T next = database.get(dbIter.next());
                if(mean == null)
                {
                    mean = next;
                }
                else
                {
                    mean = (T) mean.plus(next);
                }
            }
            if(list.size() > 0)
            {
                mean = (T) mean.multiplicate(1.0 / list.size());
            }
            means.add(mean);
        }
        return means;
    }

    /**
     * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids
     * of those FeatureVectors, that are nearest to the
     * k<sup>th</sup> mean.
     * 
     * 
     * @param means a list of k means
     * @param database the database to cluster
     * @return list of k clusters
     */
    protected List<List<Integer>> sort(List<T> means, Database<T> database)
    {
        List<List<Integer>> clusters = new ArrayList<List<Integer>>();
        for(Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();)
        {
            Distance[] distances = new Distance[k];
            Integer id = dbIter.next();
            T fv = database.get(id);
            int minIndex = 0;
            for(int d = 0; d < k; d++)
            {
                distances[d] = getDistanceFunction().distance(fv, means.get(k));
                if(distances[d].compareTo(distances[minIndex]) < 0)
                {
                    minIndex = d;
                }
            }
            clusters.get(minIndex).add(id);
        }
        for(Iterator<List<Integer>> clustersIter = clusters.iterator(); clustersIter.hasNext();)
        {
            Collections.sort(clustersIter.next());
        }
        return clusters;
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            k = Integer.parseInt(optionHandler.getOptionValue(K_P));
            if(k <= 0)
            {
                throw new IllegalArgumentException("Parameter "+K_P+" must be a positive integer.");
            }
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingParameters;
    }
    
}
