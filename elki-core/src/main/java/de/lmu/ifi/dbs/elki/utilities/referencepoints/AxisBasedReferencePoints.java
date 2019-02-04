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
package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Strategy to pick reference points by placing them on the axis ends.
 * 
 * This strategy produces n+2 reference points that lie on the edges of the
 * surrounding cube.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class AxisBasedReferencePoints implements ReferencePointsHeuristic {
  /**
   * Scaling factor.
   */
  protected double spacescale;

  /**
   * Constructor.
   * 
   * @param spacescale Extra scaling
   */
  public AxisBasedReferencePoints(double spacescale) {
    super();
    this.spacescale = spacescale;
  }

  @Override
  public Collection<? extends NumberVector> getReferencePoints(Relation<? extends NumberVector> db) {
    double[][] minmax = RelationUtil.computeMinMax(db);
    int dim = RelationUtil.dimensionality(db);

    // Compute mean and extend from minmax.
    double[] mean = minmax[0], delta = minmax[1];
    for(int d = 0; d < dim; d++) {
      delta[d] -= mean[d];
      mean[d] -= delta[d] * .5;
    }

    ArrayList<NumberVector> result = new ArrayList<>(2 + dim);

    double[] vec = new double[dim];
    // Use min and max
    for(int d = 0; d < dim; d++) {
      vec[d] = mean[d] - delta[d];
    }
    result.add(DoubleVector.copy(vec));
    for(int d = 0; d < dim; d++) {
      vec[d] = mean[d] + delta[d];
    }
    result.add(DoubleVector.copy(vec));

    // Plus axis end points:
    for(int i = 0; i < dim; i++) {
      for(int d = 0; d < dim; d++) {
        if(d != i) {
          vec[d] = mean[d] - delta[d];
        }
        else {
          vec[d] = mean[d] + delta[d];
        }
      }
      result.add(DoubleVector.copy(vec));
    }

    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the extra scaling of the space, to allow
     * out-of-data-space reference points.
     */
    public static final OptionID SPACE_SCALE_ID = new OptionID("axisref.scale", "Scale the data space extension by the given factor.");

    /**
     * Holds the value of {@link #SPACE_SCALE_ID}.
     */
    protected double spacescale = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter spacescaleP = new DoubleParameter(SPACE_SCALE_ID, 1.0)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(spacescaleP)) {
        spacescale = spacescaleP.getValue();
      }
    }

    @Override
    protected AxisBasedReferencePoints makeInstance() {
      return new AxisBasedReferencePoints(spacescale);
    }
  }
}
