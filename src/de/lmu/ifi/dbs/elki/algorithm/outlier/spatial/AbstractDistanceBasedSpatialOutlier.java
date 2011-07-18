package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for distance-based spatial outlier detection methods.
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Object type for neighborhood
 * @param <O> Non-spatial object type
 * @param <D> Distance value type
 */
public abstract class AbstractDistanceBasedSpatialOutlier<N, O, D extends NumberDistance<D, ?>> extends AbstractNeighborhoodOutlier<N> {
  /**
   * Parameter to specify the non spatial distance function to use
   */
  public static final OptionID NON_SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("slom.nonspatialdistancefunction", "The distance function to use for non spatial attributes");

  /**
   * Holds the value of {@link #NON_SPATIAL_DISTANCE_FUNCTION_ID}
   */
  private PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction;

  /**
   * Constructor.
   * 
   * @param npredf Neighborhood predicate factory
   * @param nonSpatialDistanceFunction Distance function to use on the
   *        non-spatial attributes.
   */
  public AbstractDistanceBasedSpatialOutlier(NeighborSetPredicate.Factory<N> npredf, PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction) {
    super(npredf);
    this.nonSpatialDistanceFunction = nonSpatialDistanceFunction;
  }

  /**
   * Get the non-spatial relation
   * 
   * @return the distance function to use on the non-spatial attributes
   */
  protected PrimitiveDistanceFunction<O, D> getNonSpatialDistanceFunction() {
    return nonSpatialDistanceFunction;
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Object type for neighborhood
   * @param <O> Non-spatial object type
   * @param <D> Distance value type
   */
  public static abstract class Parameterizer<N, O, D extends NumberDistance<D, ?>> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    /**
     * The distance function to use on the non-spatial attributes.
     */
    protected PrimitiveDistanceFunction<O, D> distanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistanceFunction<O, D>> distanceFunctionP = makeParameterDistanceFunction(EuclideanDistanceFunction.class, PrimitiveDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }
  }
}