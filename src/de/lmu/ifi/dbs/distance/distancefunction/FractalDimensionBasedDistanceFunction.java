package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalFractalDimensionCluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.preprocessing.FracClusPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.regex.Pattern;

/**
 * @author Arthur Zimek
 */
public class FractalDimensionBasedDistanceFunction<V extends RealVector<V, ?>> extends AbstractPreprocessorBasedDistanceFunction<V, FracClusPreprocessor<V>, DoubleDistance>
{
    private static final EuklideanDistanceFunction STANDARD_DOUBLE_DISTANCE_FUNCTION = new EuklideanDistanceFunction();
    
    public FractalDimensionBasedDistanceFunction()
    {
        super(Pattern.compile(STANDARD_DOUBLE_DISTANCE_FUNCTION.requiredInputPattern()));
    }
    
    public DoubleDistance distance(V o1, V o2)
    {
        return new DoubleDistance(
                new HierarchicalFractalDimensionCluster<V>
                ((HierarchicalFractalDimensionCluster<V>) this.getDatabase().getAssociation(this.getAssociationID(), o1.getID()),
                 (HierarchicalFractalDimensionCluster<V>) this.getDatabase().getAssociation(this.getAssociationID(), o2.getID()),
                 this.getDatabase(),
                 this.getPreprocessor().getK()).getFractalDimension());
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
    Class getPreprocessorSuperClassName()
    {
        return FracClusPreprocessor.class;
    }


    
}
