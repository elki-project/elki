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
package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Edit Distance for FeatureVectors.
 * 
 * @author Thomas Bernecker
 * @since 0.2
 */
public abstract class AbstractEditDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Keeps the currently set bandSize.
   */
  protected double bandSize;

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   */
  public AbstractEditDistanceFunction(double bandSize) {
    super();
    this.bandSize = bandSize;
  }

  /**
   * Compute the effective band size.
   * 
   * @param dim1 First dimensionality
   * @param dim2 Second dimensionality
   * @return Effective bandsize
   */
  protected int effectiveBandSize(final int dim1, final int dim2) {
    if(bandSize == Double.POSITIVE_INFINITY) {
      return (dim1 > dim2) ? dim1 : dim2;
    }
    if(bandSize >= 1.) {
      return (int) bandSize;
    }
    // Max * bandSize:
    return (int) Math.ceil((dim1 >= dim2 ? dim1 : dim2) * bandSize);
  }

  @Override
  public VectorTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.VARIABLE_LENGTH;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj != null && this.getClass().equals(obj.getClass()) //
        && this.bandSize == ((AbstractEditDistanceFunction) obj).bandSize);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() * 31 + Double.hashCode(bandSize);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Bandsize parameter.
     */
    public static final OptionID BANDSIZE_ID = new OptionID("edit.bandsize", //
        "The band size for time series alignment. By default, no constraint is used. "//
            + "If the value is larger than 0, it will be considered absolute, otherwise relative to the longer sequence. " //
            + "Note that 0 does not make sense: use Euclidean distance then instead.");

    /**
     * Keeps the currently set bandSize.
     */
    protected double bandSize = Double.POSITIVE_INFINITY;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter bandSizeP = new DoubleParameter(BANDSIZE_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(bandSizeP)) {
        bandSize = bandSizeP.doubleValue();
      }
    }
  }
}
