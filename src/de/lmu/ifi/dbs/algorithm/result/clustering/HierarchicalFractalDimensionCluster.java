package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.math.statistics.LinearRegression;
import de.lmu.ifi.dbs.utilities.DoublePair;
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
    
    private EuklideanDistanceFunction<V> distanceFunction;
    
    private double fractalDimension;
    
    public HierarchicalFractalDimensionCluster(Integer pointID, Database<V> database, int k)
    {
        final int NUMBER_STRONG_SUPPORTERS = (k+1)/2;
        this.representant = database.get(pointID);
        this.distanceFunction = new EuklideanDistanceFunction<V>();
        distanceFunction.setDatabase(database, false, false); //  TODO: parameters verbose, time???
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
        this.fractalDimension = this.computeFractalDimension();
        this.setLabel("Fractal_Dimension="+this.fractalDimension);
    }
    
    public HierarchicalFractalDimensionCluster(HierarchicalFractalDimensionCluster<V> cluster1, HierarchicalFractalDimensionCluster<V> cluster2, Database<V> database, int k)
    {
        final int NUMBER_STRONG_SUPPORTERS = (k+1)/2;
        this.representant = cluster1.getRepresentant().multiplicate(cluster1.size())
                            .plus(cluster2.getRepresentant().multiplicate(cluster2.size()))
                            .multiplicate(1.0/(cluster1.size()+cluster2.size()));
        
        this.distanceFunction = new EuklideanDistanceFunction<V>();
        distanceFunction.setDatabase(database, false, false); //  TODO: parameters verbose, time???
        KNNList<DoubleDistance> knnList = new KNNList<DoubleDistance>(k, distanceFunction.infiniteDistance());

        for(Integer id : cluster1.getStrongSupporters())
        {
            knnList.add(new QueryResult<DoubleDistance>(id, distanceFunction.distance(id, this.representant)));
        }
        for(Integer id : cluster2.getStrongSupporters())
        {
            knnList.add(new QueryResult<DoubleDistance>(id, distanceFunction.distance(id, this.representant)));
        }
        
        this.supporters = new ArrayList<Integer>(k);
        List<QueryResult<DoubleDistance>> ids = knnList.toList();
        for(QueryResult<DoubleDistance> qr : ids)
        {
            this.supporters.add(qr.getID());
        }
        this.strongSupporters = new ArrayList<Integer>(NUMBER_STRONG_SUPPORTERS);
        
        for(int i = 0; i < NUMBER_STRONG_SUPPORTERS; i++)
        {
            this.strongSupporters.add(ids.get(i).getID());
        }
        this.addIDs(cluster1.getIDs());
        this.addIDs(cluster2.getIDs());
        this.addChild(cluster1);
        this.addChild(cluster2);
        this.fractalDimension = this.computeFractalDimension();
        this.setLabel("Fractal_Dimension="+this.fractalDimension);
    }

    private double computeFractalDimension()
    {
        List<DoublePair> points = new ArrayList<DoublePair>(this.getSupporters().size());
        for(int i = 1; i <= this.supporters.size(); i++)
        {
            points.add(new DoublePair(Math.log(distanceFunction.distance(this.supporters.get(i-1), this.representant).getDoubleValue()),Math.log(i)));
        }
        return new LinearRegression(points).getM();
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
    

    public double getFractalDimension()
    {
        return this.fractalDimension;
    }
    
    
}
