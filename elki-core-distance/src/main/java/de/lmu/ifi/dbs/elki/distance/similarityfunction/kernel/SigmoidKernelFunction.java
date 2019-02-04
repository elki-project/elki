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
package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractVectorSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import net.jafama.FastMath;

/**
 * Sigmoid kernel function (aka: hyperbolic tangent kernel, multilayer
 * perceptron MLP kernel).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias("sigmoid")
public class SigmoidKernelFunction extends AbstractVectorSimilarityFunction {
  /**
   * Scaling factor c, bias theta
   */
  private final double c, theta;

  /**
   * Constructor.
   * 
   * @param c Scaling factor c.
   * @param theta Bias parameter theta.
   */
  public SigmoidKernelFunction(double c, double theta) {
    super();
    this.c = c;
    this.theta = theta;
  }

  @Override
  public double similarity(NumberVector o1, NumberVector o2) {
    final int dim = AbstractNumberVectorDistanceFunction.dimensionality(o1, o2);
    double sim = 0.;
    for(int i = 0; i < dim; i++) {
      final double v = o1.doubleValue(i) * o2.doubleValue(i);
      sim += v;
    }
    return FastMath.tanh(c * sim + theta);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * C parameter: scaling
     */
    public static final OptionID C_ID = new OptionID("kernel.sigmoid.c", "Sigmoid c parameter (scaling).");

    /**
     * Theta parameter: bias
     */
    public static final OptionID THETA_ID = new OptionID("kernel.sigmoid.theta", "Sigmoid theta parameter (bias).");

    /**
     * C parameter, theta parameter
     */
    protected double c = 1., theta = 0.;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter cP = new DoubleParameter(C_ID, 1.);
      if(config.grab(cP)) {
        c = cP.doubleValue();
      }
      final DoubleParameter thetaP = new DoubleParameter(THETA_ID, 0.);
      if(config.grab(thetaP)) {
        theta = thetaP.doubleValue();
      }
    }

    @Override
    protected SigmoidKernelFunction makeInstance() {
      return new SigmoidKernelFunction(c, theta);
    }
  }
}
