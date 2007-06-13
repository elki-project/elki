package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Arthur Zimek
 */
public class HierarchicalFractalDimensionCluster<V extends RealVector<V,?>> extends HierarchicalCluster<HierarchicalFractalDimensionCluster<V>>
{
    private V representant;
    
    private List<Integer> supporters;
    
    private List<Integer> strongSupporters;
    
    public HierarchicalFractalDimensionCluster(Integer pointID, Database<V> database, int k)
    {
        final int NUMBER_STRONG_SUPPORTERS = (k+1)/2;
        this.representant = database.get(pointID);
        EuklideanDistanceFunction<V> distanceFunction = new EuklideanDistanceFunction<V>();
        distanceFunction.setDatabase(database, this.debug, false); //  TODO: parameters verbose, time???
        List<QueryResult<DoubleDistance>> kNN = database.kNNQueryForID(pointID, k+1, distanceFunction);
        this.supporters = new ArrayList<Integer>(k);
        this.strongSupporters = new ArrayList<Integer>(NUMBER_STRONG_SUPPORTERS);

        // begin from 1: 0th element is the very same object
        for(int i = 1; i < kNN.size(); i++)
        {
            QueryResult<DoubleDistance> ithQueryResult = kNN.get(i);
            this.supporters.add(ithQueryResult.getID());
            if(i <= NUMBER_STRONG_SUPPORTERS)
            {
                this.strongSupporters.add(ithQueryResult.getID());
            }
        }        
        this.addID(pointID);
    }
    
    public HierarchicalFractalDimensionCluster(HierarchicalFractalDimensionCluster<V> cluster1, HierarchicalFractalDimensionCluster<V> cluster2, Database<V> database, int k)
    {
        final int NUMBER_STRONG_SUPPORTERS = (k+1)/2;
        this.representant = cluster1.getRepresentant().multiplicate(cluster1.size())
                            .plus(cluster2.getRepresentant().multiplicate(cluster2.size()))
                            .multiplicate(1.0/(cluster1.size()+cluster2.size()));
        this.supporters = new ArrayList<Integer>(cluster1.getStrongSupporters());
        this.supporters.addAll(cluster2.getStrongSupporters());
        EuklideanDistanceFunction<V> distanceFunction = new EuklideanDistanceFunction<V>();
        distanceFunction.setDatabase(database, this.debug, false); //  TODO: parameters verbose, time???
        KNNList<DoubleDistance> knnList = new KNNList<DoubleDistance>(NUMBER_STRONG_SUPPORTERS, distanceFunction.infiniteDistance());
        for(Integer id : this.supporters)
        {
            knnList.add(new QueryResult<DoubleDistance>(id, distanceFunction.distance(id, this.representant)));
        }
        this.strongSupporters = new ArrayList<Integer>(NUMBER_STRONG_SUPPORTERS);
        for(QueryResult<DoubleDistance> qr : knnList.toList())
        {
            this.strongSupporters.add(qr.getID());
        }
        this.addIDs(cluster1.getIDs());
        this.addIDs(cluster2.getIDs());
    }

    public double fractalDimension()
    {
        // TODO
        return 0;
    }

    public V getRepresentant()
    {
        return this.representant;
    }

    public List<Integer> getSupporters()
    {
        return this.supporters;
    }
    
    

    public List<Integer> getStrongSupporters()
    {
        return this.strongSupporters;
    }

    /**
     * The number of represented database objects.
     * 
     * @return number of represented database objects
     */
    public int size()
    {
        return this.getIDs().size();
    }
}
