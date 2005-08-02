package de.lmu.ifi.dbs.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DBSCAN extends DistanceBasedAlgorithm
{
    public static final String EPSILON_P = "epsilon";
    
    public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the specified distance function";

    public static final String MINPTS_P = "minpts";
    
    public static final String MINPTS_D = "<int>minpts";
    
    protected String epsilon;
    
    protected int minpts;
    
    private List<List<Integer>> result;
    
    private Set<Integer> noise;
    
    private Set<Integer> processedIDs;
    
    /**
     * Sets epsilon
     */
    public DBSCAN()
    {
        super();
        parameterToDescription.put(EPSILON_P+OptionHandler.EXPECTS_VALUE,EPSILON_D);
        parameterToDescription.put(MINPTS_P+OptionHandler.EXPECTS_VALUE,MINPTS_D);
        optionHandler = new OptionHandler(parameterToDescription,DBSCAN.class.getName());
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public void run(Database database) throws IllegalStateException
    {
        try
        {
            Progress progress = new Progress(database.size());
            result = new ArrayList<List<Integer>>();
            noise = new HashSet<Integer>();
            processedIDs = new HashSet<Integer>(database.size());
            
            for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
            {
                Integer id = iter.next();
                if(!processedIDs.contains(id))
                {
                    expandCluster(database, id);
                }
            }
            if(isVerbose())
            {
                progress.setProcessed(processedIDs.size());
                System.out.println("\r"+progress.toString()+" Number of clusters: "+result.size()+".                           ");
            }
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e.getMessage());
        }
    }

    
    /**
     * DBSCAN-function expandCluster.
     * 
     * Border-Objects become members of the first possible cluster.
     * 
     * @param startObjectID potential seed of a new potential cluster 
     * @return boolean true if a cluster was extended successfully
     */
    protected boolean expandCluster(Database database, Integer startObjectID)
    {
        Set<Integer> processedIDsOLD = new HashSet<Integer>(processedIDs);
        Set<Integer> noiseOLD = new HashSet<Integer>(noise);
        
        List<QueryResult> seeds = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());
        if(seeds.size() < minpts)
        {
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
            return false;
        }
        else
        {
            List<Integer> currentCluster = new ArrayList<Integer>();
            Iterator<QueryResult> seedsIterator = seeds.iterator();
            while(seedsIterator.hasNext())
            {
                Integer nextID = seedsIterator.next().getId();
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
            seeds.remove(startObjectID);
            
            while(seeds.size() > 0)
            {
                Integer o = seeds.remove(0).getId();
                List<QueryResult> neighborhood = database.rangeQuery(o, epsilon, getDistanceFunction());
                if(neighborhood.size() >= minpts)
                {
                    for(int n = 0; n < neighborhood.size(); n++)
                    {
                        Integer p = neighborhood.get(n).getId();
                        boolean inNoise = noise.contains(p);
                        boolean unclassified = !processedIDs.contains(p);
                        if(inNoise || unclassified)
                        {
                            if(unclassified)
                            {
                                seeds.add(neighborhood.get(n));
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
            }
            if(currentCluster.size() >= minpts)
            {
                result.add(currentCluster);
                return true;
            }
            else
            {
                processedIDs = processedIDsOLD;
                noise = noiseOLD;
                noise.add(startObjectID);
                processedIDs.add(startObjectID);
                return false;
            }
        }
    }

    
    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("DBSCAN",
                "Density-Based Clustering of Applications with Noise",
                "Algorithm to find density-connected sets in a database based on the parameters minimumPoints and epsilon (specifying a volume). These two parameters determine a density threshold for clustering.",
                "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. In: Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996.");
    }

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
            throw new IllegalArgumentException(e.getMessage());
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        return remainingParameters;
    }

}
