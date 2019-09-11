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
package elki.distance.subspace;

import java.util.Arrays;

import elki.data.FeatureVector;
import elki.distance.PrimitiveDistance;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Abstract base class for distances computed only in subspaces.
 * 
 * Selected dimensions are encuded as bits in a {@code long[]}.
 * 
 * @author Elke Achtert
 * @since 0.1
 *
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public abstract class AbstractDimensionsSelectingDistance<V extends FeatureVector<?>> implements PrimitiveDistance<V>, DimensionSelectingSubspaceDistance<V> {
  /**
   * The dimensions to be considered for distance computation.
   */
  protected long[] dimensions;

  /**
   * Constructor.
   * 
   * @param dimensions
   */
  public AbstractDimensionsSelectingDistance(long[] dimensions) {
    super();
    this.dimensions = dimensions;
  }

  @Override
  public double distance(V o1, V o2) {
    return distance(o1, o2);
  }

  @Override
  public long[] getSelectedDimensions() {
    return dimensions;
  }

  @Override
  public void setSelectedDimensions(long[] dimensions) {
    this.dimensions = dimensions;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()) && //
        Arrays.equals(this.dimensions, ((AbstractDimensionsSelectingDistance<?>) obj).dimensions));
  }

  @Override
  public int hashCode() {
    return this.getClass().hashCode() + BitsUtil.hashCode(dimensions);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Dimensions parameter.
     */
    public static final OptionID DIMS_ID = new OptionID("distance.dims", "a comma separated array of integer values, where 0 <= d_i < the dimensionality of the feature space specifying the dimensions to be considered for distance computation. If this parameter is not set, no dimensions will be considered, i.e. the distance between two objects is always 0.");

    /**
     * Bitmask of the dimensions
     */
    protected long[] dimensions = null;

    @Override
    public void configure(Parameterization config) {
      new IntListParameter(DIMS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST) //
          .setOptional(true) //
          .grab(config, x -> BitsUtil.of(x));
    }
  }
}
