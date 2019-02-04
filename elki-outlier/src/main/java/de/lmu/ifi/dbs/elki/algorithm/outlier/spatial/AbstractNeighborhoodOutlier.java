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
package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for spatial outlier detection methods using a spatial
 * neighborhood.
 * 
 * @author Ahmed Hettab
 * @since 0.4.0
 * 
 * @param <O> Object type
 */
public abstract class AbstractNeighborhoodOutlier<O> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = new OptionID("neighborhood", "The neighborhood predicate to use in comparison step.");

  /**
   * Our predicate to obtain the neighbors
   */
  private NeighborSetPredicate.Factory<O> npredf = null;

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   */
  public AbstractNeighborhoodOutlier(NeighborSetPredicate.Factory<O> npredf) {
    super();
    this.npredf = npredf;
  }

  /**
   * Get the predicate to obtain the neighbors.
   * 
   * @return predicate to obtain the neighbors
   */
  protected NeighborSetPredicate.Factory<O> getNeighborSetPredicateFactory() {
    return npredf;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> Object type
   */
  public abstract static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * The predicate to obtain the neighbors.
     */
    protected NeighborSetPredicate.Factory<O> npredf = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<NeighborSetPredicate.Factory<O>> param = new ObjectParameter<>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class);
      if(config.grab(param)) {
        npredf = param.instantiateClass(config);
      }
    }
  }
}