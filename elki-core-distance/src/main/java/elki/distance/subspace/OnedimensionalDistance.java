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

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.data.type.VectorFieldTypeInformation;
import elki.data.type.VectorTypeInformation;
import elki.distance.AbstractNumberVectorDistance;
import elki.distance.Norm;
import elki.distance.SpatialPrimitiveDistance;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Distance function that computes the distance between feature vectors as the
 * absolute difference of their values in a specified dimension only.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class OnedimensionalDistance extends AbstractNumberVectorDistance implements SpatialPrimitiveDistance<NumberVector>, DimensionSelectingSubspaceDistance<NumberVector>, Norm<NumberVector> {
  /**
   * The dimension to be considered for distance computation.
   */
  private int dim;

  /**
   * Constructor.
   * 
   * @param dim Dimension
   */
  public OnedimensionalDistance(int dim) {
    super();
    this.dim = dim;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    if(dim >= v1.getDimensionality() || dim >= v2.getDimensionality() || dim < 0) {
      throw new IllegalArgumentException("Specified dimension to be considered " + "is larger that dimensionality of FeatureVectors:" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n  dimension: " + dim);
    }

    double delta = v1.doubleValue(dim) - v2.doubleValue(dim);
    return delta >= 0 ? delta : -delta;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(dim >= mbr1.getDimensionality() || dim >= mbr2.getDimensionality() || dim < 0) {
      throw new IllegalArgumentException("Specified dimension to be considered " + "is larger that dimensionality of FeatureVectors:" + "\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString() + "\n  dimension: " + dim);
    }

    final double max1 = mbr1.getMax(dim), min2 = mbr2.getMin(dim);
    if(max1 < min2) {
      return min2 - max1;
    }
    final double min1 = mbr1.getMin(dim), max2 = mbr2.getMax(dim);
    if(min1 > max2) {
      return min1 - max2;
    }
    return 0;
  }

  @Override
  public double norm(NumberVector obj) {
    return Math.abs(obj.doubleValue(dim));
  }

  /**
   * Returns the selected dimension.
   * 
   * @return the selected dimension
   */
  public int getSelectedDimension() {
    return dim;
  }

  @Override
  @Deprecated
  public long[] getSelectedDimensions() {
    long[] bs = BitsUtil.zero(dim);
    BitsUtil.setI(bs, dim);
    return bs;
  }

  @Override
  @Deprecated
  public void setSelectedDimensions(long[] dimensions) {
    dim = BitsUtil.nextSetBit(dimensions, 0);
    if(dim == -1) {
      throw new IllegalArgumentException("No dimension was set.");
    }
    if(BitsUtil.nextSetBit(dimensions, dim + 1) > 0) {
      throw new IllegalArgumentException("More than one dimension was set.");
    }
  }

  @Override
  public VectorTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return VectorFieldTypeInformation.typeRequest(NumberVector.class, dim, Integer.MAX_VALUE);
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()) && //
        this.dim == ((OnedimensionalDistance) obj).dim);
  }

  @Override
  public int hashCode() {
    return this.getClass().hashCode() + dim * 31;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for dimensionality.
     */
    public static final OptionID DIM_ID = new OptionID("dim", "an integer between 1 and the dimensionality of the " + "feature space 1 specifying the dimension to be considered " + "for distance computation.");

    /**
     * Selected dimension.
     */
    protected int dim = 0;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(DIM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> dim = x);
    }

    @Override
    public OnedimensionalDistance make() {
      return new OnedimensionalDistance(dim);
    }
  }
}
