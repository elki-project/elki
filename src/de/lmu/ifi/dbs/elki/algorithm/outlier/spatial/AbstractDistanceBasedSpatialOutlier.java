package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
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
  public static final OptionID NON_SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("spatialoutlier.nonspatialdistance", "The distance function to use for non spatial attributes");

  /**
   * The distance function to use
   */
  private DistanceFunction<O, D> nonSpatialDistanceFunction;

  /**
   * Constructor.
   * 
   * @param npredf Neighborhood predicate factory
   * @param nonSpatialDistanceFunction Distance function to use on the
   *        non-spatial attributes.
   */
  public AbstractDistanceBasedSpatialOutlier(NeighborSetPredicate.Factory<N> npredf, DistanceFunction<O, D> nonSpatialDistanceFunction) {
    super(npredf);
    this.nonSpatialDistanceFunction = nonSpatialDistanceFunction;
  }

  /**
   * Get the non-spatial relation
   * 
   * @return the distance function to use on the non-spatial attributes
   */
  protected DistanceFunction<O, D> getNonSpatialDistanceFunction() {
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