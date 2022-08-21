/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.similarity.kernel;

import elki.data.NumberVector;
import elki.distance.AbstractNumberVectorDistance;
import elki.similarity.AbstractVectorSimilarity;
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import net.jafama.FastMath;

/**
 * Gaussian radial basis function kernel (RBF Kernel).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Priority(Priority.RECOMMENDED)
@Alias("rbf")
public class RadialBasisFunctionKernel extends AbstractVectorSimilarity {
  /**
   * Scaling factor mgamma = -1/(2 sigma^2).
   */
  private final double mgamma;

  /**
   * Constructor.
   * 
   * @param gamma Scaling parameter gamma
   */
  public RadialBasisFunctionKernel(double gamma) {
    super();
    this.mgamma = -gamma;
  }

  @Override
  public double similarity(NumberVector o1, NumberVector o2) {
    final int dim = AbstractNumberVectorDistance.dimensionality(o1, o2);
    double sim = 0.;
    for(int i = 0; i < dim; i++) {
      final double v = o1.doubleValue(i) - o2.doubleValue(i);
      sim += v * v;
    }
    return FastMath.exp(mgamma * sim);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Gamma parameter.
     */
    public static final OptionID GAMMA_ID = new OptionID("kernel.rbf.gamma", "Gamma parameter of the Gaussian RBF kernel (alternative to sigma).");

    /**
     * Sigma parameter: standard deviation.
     */
    public static final OptionID SIGMA_ID = new OptionID("kernel.rbf.sigma", "Standard deviation of the Gaussian RBF kernel (alternative to gamma).");

    /**
     * Gamma scaling parameter
     */
    protected double gamma = .5;

    @Override
    public void configure(Parameterization config) {
      DoubleParameter g = new DoubleParameter(GAMMA_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setOptional(true);
      if(config.grab(g)) {
        gamma = g.doubleValue();
      }
      else {
        new DoubleParameter(SIGMA_ID, 1.) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .grab(config, x -> gamma = .5 / (x * x));
      }
    }

    @Override
    public RadialBasisFunctionKernel make() {
      return new RadialBasisFunctionKernel(gamma);
    }
  }
}
