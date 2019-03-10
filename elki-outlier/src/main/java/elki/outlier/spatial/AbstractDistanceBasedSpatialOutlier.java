/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.outlier.spatial;

import elki.outlier.spatial.neighborhood.NeighborSetPredicate;
import elki.DistanceBasedAlgorithm;
import elki.distance.Distance;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for distance-based spatial outlier detection methods.
 * 
 * @author Ahmed Hettab
 * @since 0.4.0
 * 
 * @param <N> Object type for neighborhood
 * @param <O> Non-spatial object type
 */
public abstract class AbstractDistanceBasedSpatialOutlier<N, O> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The distance function to use
   */
  private Distance<O> nonSpatialDistance;

  /**
   * Constructor.
   * 
   * @param npredf Neighborhood predicate factory
   * @param nonSpatialDistance Distance function to use on the
   *        non-spatial attributes.
   */
  public AbstractDistanceBasedSpatialOutlier(NeighborSetPredicate.Factory<N> npredf, Distance<O> nonSpatialDistance) {
    super(npredf);
    this.nonSpatialDistance = nonSpatialDistance;
  }

  /**
   * Get the non-spatial relation
   * 
   * @return the distance function to use on the non-spatial attributes
   */
  protected Distance<O> getNonSpatialDistance() {
    return nonSpatialDistance;
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @hidden
   * 
   * @param <N> Object type for neighborhood
   * @param <O> Non-spatial object type
   */
  public abstract static class Parameterizer<N, O> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    /**
     * Parameter to specify the non spatial distance function to use
     */
    public static final OptionID NON_SPATIAL_DISTANCE_FUNCTION_ID = new OptionID("spatialoutlier.nonspatialdistance", "The distance function to use for non spatial attributes");

    /**
     * The distance function to use on the non-spatial attributes.
     */
    protected PrimitiveDistance<O> distanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistance<O>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, PrimitiveDistance.class, EuclideanDistance.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }
  }
}
