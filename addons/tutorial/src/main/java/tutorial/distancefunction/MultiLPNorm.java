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
package tutorial.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Tutorial example Minowski-distance variation with different exponents for
 * different dimensions for ELKI.
 * <p>
 * See
 * <a href="https://elki-project.github.io/tutorial/distance_functions">Distance
 * function tutorial</a>
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class MultiLPNorm extends AbstractNumberVectorDistanceFunction {
  /**
   * The exponents
   */
  double[] ps;

  /**
   * Normalization factor (count(ps)/sum(ps))
   */
  double pinv;

  /**
   * Constructor.
   * 
   * @param ps
   *        The exponents
   */
  public MultiLPNorm(double[] ps) {
    super();
    double sum = 0.0;
    for(int dim = 0; dim < ps.length; dim++) {
      assert (ps[dim] >= 0) : "Negative exponents are not allowed.";
      sum += ps[dim];
    }
    assert (sum > 0) : "At least one exponent should be different from 0!";

    this.ps = ps;
    this.pinv = ps.length / sum;
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    assert o1.getDimensionality() == ps.length : "Inappropriate dimensionality!";
    assert o2.getDimensionality() == ps.length : "Inappropriate dimensionality!";

    double sum = 0.0;
    for(int dim = 0; dim < ps.length; dim++) {
      if(ps[dim] > 0) {
        final double delta = Math.abs(o1.doubleValue(dim) - o2.doubleValue(dim));
        sum += Math.pow(delta, ps[dim]);
      }
    }
    return Math.pow(sum, pinv);
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return VectorFieldTypeInformation.typeRequest(NumberVector.class, ps.length, ps.length);
  }

  /**
   * Parameterization class example
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option ID for the exponents
     */
    public static final OptionID EXPONENTS_ID = new OptionID("multinorm.ps", "The exponents to use for this distance function");

    /**
     * P exponents
     */
    double[] ps;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleListParameter ps_param = new DoubleListParameter(EXPONENTS_ID);
      if(config.grab(ps_param)) {
        ps = ps_param.getValue().clone();
      }
    }

    @Override
    protected MultiLPNorm makeInstance() {
      return new MultiLPNorm(ps);
    }
  }
}
