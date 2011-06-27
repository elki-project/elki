package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;
/**
 * 
 * @author Ahmed Hettab
 *
 * @param <V>
 * @param <D>
 */
public abstract class DistanceBasedSpatialOutlier<V extends NumberVector<?,?>,D extends NumberDistance<D, ?>> extends AbstractNeighborhoodOutlier<V> {
  /**
   * Parameter to specify the non spatial distance function to use
   */
  public static final OptionID NON_SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("slom.nonspatialdistancefunction", "The distance function to use for non spatial attributes");
  /**
   * Holds the value of {@link #NON_SPATIAL_DISTANCE_FUNCTION_ID}
   */
  private PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction;
 /**
  * 
  * @param npredf
  * @param nonSpatialDistanceFunction
  */
  public DistanceBasedSpatialOutlier(NeighborSetPredicate.Factory<V> npredf, PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction) {
    super(npredf);
    this.nonSpatialDistanceFunction = nonSpatialDistanceFunction;
  }
  /**
   * 
   * @return the non Spatial Distance Function
   */
  public PrimitiveDistanceFunction<V, D> getNonSpatialDistanceFunction(){
    return nonSpatialDistanceFunction ;
  }
  
  public static abstract class Parameterizer<V extends NumberVector<?,?>,D extends NumberDistance<D, ?>> extends AbstractNeighborhoodOutlier.Parameterizer<V>{
    
    protected PrimitiveDistanceFunction<V, D> distanceFunction = null ;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistanceFunction<V, D>> distanceFunctionP = makeParameterDistanceFunction(DimensionsSelectingEuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }

   
    
  }
    
  
 
 
  
}
