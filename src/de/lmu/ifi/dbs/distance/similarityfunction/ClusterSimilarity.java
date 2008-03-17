package de.lmu.ifi.dbs.distance.similarityfunction;

import de.lmu.ifi.dbs.algorithm.result.clustering.Cluster;
import de.lmu.ifi.dbs.distance.IntegerDistance;

import java.util.Arrays;

/**
 * @author Arthur Zimek
 */
public class ClusterSimilarity extends AbstractIntegerSimilarityFunction<Cluster<?>>
{

    public IntegerDistance similarity(Integer id1, Integer id2)
    {
        Cluster<?> cluster1 = getDatabase().get(id1);
        Cluster<?> cluster2 = getDatabase().get(id2);
        return similarity(cluster1,cluster2);
    }

    @Override
    public IntegerDistance similarity(Cluster<?> o1, Cluster<?> o2)
    {
        int[] ids1 = o1.getClusterIDs();
        int[] ids2 = o2.getClusterIDs();
        int intersection = 0;
        for(int i : ids1)
        {
            if(Arrays.binarySearch(ids2, i) >= 0)
            {
                intersection++;
            }
        }
        return new IntegerDistance(intersection);
    }

    @Override
    public IntegerDistance similarity(Integer id1, Cluster<?> cluster2)
    {
        Cluster<?> cluster1 = getDatabase().get(id1);
        return super.similarity(cluster1, cluster2);
    }
    
    

}
