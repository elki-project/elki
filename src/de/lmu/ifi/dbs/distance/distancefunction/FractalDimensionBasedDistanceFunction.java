package de.lmu.ifi.dbs.distance.distancefunction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.math.statistics.LinearRegression;
import de.lmu.ifi.dbs.preprocessing.FracClusPreprocessor;
import de.lmu.ifi.dbs.utilities.DoublePair;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;

/**
 * @author Arthur Zimek
 */
public class FractalDimensionBasedDistanceFunction<V extends RealVector<V, ?>> extends AbstractPreprocessorBasedDistanceFunction<V, FracClusPreprocessor<V>, DoubleDistance>
{
    public final EuklideanDistanceFunction<V> STANDARD_DOUBLE_DISTANCE_FUNCTION = new EuklideanDistanceFunction<V>();
        
    public FractalDimensionBasedDistanceFunction()
    {
        super(Pattern.compile(new EuklideanDistanceFunction<V>().requiredInputPattern()));
    }
    
    public DoubleDistance distance(V o1, V o2)
    {
        List<Integer> neighbors1 = (List<Integer>) this.getDatabase().getAssociation(this.getAssociationID(), o1.getID());
        List<Integer> neighbors2 = (List<Integer>) this.getDatabase().getAssociation(this.getAssociationID(), o2.getID());
        
        Set<Integer> supporters = new HashSet<Integer>();
        supporters.addAll(neighbors1);
        supporters.addAll(neighbors2);
        
        V centroid = o1.plus(o2).multiplicate(0.5);
        
        KNNList<DoubleDistance> knnList = new KNNList<DoubleDistance>(this.getPreprocessor().getK(), STANDARD_DOUBLE_DISTANCE_FUNCTION.infiniteDistance());
        for(Integer id : supporters)
        {
            knnList.add(new QueryResult<DoubleDistance>(id, STANDARD_DOUBLE_DISTANCE_FUNCTION.distance(id, centroid)));
        }
        
        List<DoubleDistance> distances = new ArrayList<DoubleDistance>();
        
        for(QueryResult<DoubleDistance> qr : knnList.toList())
        {
            distances.add(qr.getDistance());
        }
        
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
        return AssociationID.NEIGHBORS;
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
