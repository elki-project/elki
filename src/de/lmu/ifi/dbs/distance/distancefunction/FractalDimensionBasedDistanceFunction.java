package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalFractalDimensionCluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.math.statistics.LinearRegression;
import de.lmu.ifi.dbs.preprocessing.FracClusPreprocessor;
import de.lmu.ifi.dbs.utilities.DoublePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Arthur Zimek
 */
public class FractalDimensionBasedDistanceFunction<V extends RealVector<V, ?>> extends AbstractPreprocessorBasedDistanceFunction<V, FracClusPreprocessor<V>, DoubleDistance>
{
    private final EuklideanDistanceFunction<V> STANDARD_DOUBLE_DISTANCE_FUNCTION = new EuklideanDistanceFunction<V>();
    
    public FractalDimensionBasedDistanceFunction()
    {
        super(Pattern.compile(new EuklideanDistanceFunction<V>().requiredInputPattern()));
    }
    
    public DoubleDistance distance(V o1, V o2)
    {
        HierarchicalFractalDimensionCluster<V> p1 = (HierarchicalFractalDimensionCluster<V>) this.getDatabase().getAssociation(this.getAssociationID(), o1.getID());
        HierarchicalFractalDimensionCluster<V> p2 = (HierarchicalFractalDimensionCluster<V>) this.getDatabase().getAssociation(this.getAssociationID(), o2.getID());
        
        V centroid = p1.getRepresentant().multiplicate(p1.size())
                .plus(p2.getRepresentant().multiplicate(p2.size()))
                .multiplicate(1.0/(p1.size()+p2.size()));
        
        List<DoubleDistance> distances = new ArrayList<DoubleDistance>();
        
        for(Integer id : p1.getSupporters())
        {
            distances.add(STANDARD_DOUBLE_DISTANCE_FUNCTION.distance(id, centroid));
        }
        for(Integer id : p2.getSupporters())
        {
            distances.add(STANDARD_DOUBLE_DISTANCE_FUNCTION.distance(id, centroid));
        }
        Collections.sort(distances);
        List<DoublePair> points = new ArrayList<DoublePair>(distances.size());
        for(int i = 0; i < distances.size(); i++)
        {
            points.add(new DoublePair(Math.log(distances.get(i).getDoubleValue()),Math.log(i+1)));
        }
        return new DoubleDistance(new LinearRegression(points).getM());
    }

    public DoubleDistance infiniteDistance()
    {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.infiniteDistance();
    }

    public DoubleDistance nullDistance()
    {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.nullDistance();
    }

    public DoubleDistance undefinedDistance()
    {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.undefinedDistance();
    }

    public DoubleDistance valueOf(String pattern) throws IllegalArgumentException
    {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.valueOf(pattern);
    }

    @Override
    AssociationID getAssociationID()
    {
        return AssociationID.FRACTAL_DIMENSION_CLUSTER;
    }

    @Override
    String getDefaultPreprocessorClassName()
    {
        return FracClusPreprocessor.class.getName();
    }

    @Override
    String getPreprocessorClassDescription()
    {
        return this.optionHandler.usage("");
    }

    @Override
    Class<FracClusPreprocessor> getPreprocessorSuperClassName()
    {
        return  FracClusPreprocessor.class;
    }

    @Override
    public void setDatabase(Database<V> database, boolean verbose, boolean time)
    {
        super.setDatabase(database, verbose, time);
        STANDARD_DOUBLE_DISTANCE_FUNCTION.setDatabase(this.getDatabase(), verbose, time);
    }


    
}
