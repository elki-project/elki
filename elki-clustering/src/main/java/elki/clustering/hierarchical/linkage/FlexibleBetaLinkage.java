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
package elki.clustering.hierarchical.linkage;

import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.constraints.GreaterConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Flexible-beta linkage as proposed by Lance and Williams.
 * <p>
 * Beta values larger than 0 cause chaining, and are thus not recommended.
 * Instead, choose a value between -1 and 0.
 * <p>
 * The general form of the recursive definition is:
 * \[d_{\text{Flexible},\beta}(A\cup B, C) := \tfrac{1-\beta}{2} d(A,C)
 * + \tfrac{1-\beta}{2} d(B,C) + \beta d(A,B) \]
 * <p>
 * Reference:
 * <p>
 * G. N. Lance, W. T. Williams<br>
 * A general theory of classificatory sorting strategies<br>
 * 1. Hierarchical systems<br>
 * The Computer Journal 9.4
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "G. N. Lance, W. T. Williams", //
    title = "A general theory of classificatory sorting strategies 1. Hierarchical systems", //
    booktitle = "The Computer Journal 9.4", //
    url = "https://doi.org/10.1093/comjnl/9.4.373", //
    bibkey = "doi:10.1093/comjnl/9.4.373")
@Alias({ "flex", "beta" })
@Priority(Priority.DEFAULT - 2) // Rather uncommon
public class FlexibleBetaLinkage implements Linkage {
  /**
   * Alpha parameter, derived from beta.
   */
  double alpha;

  /**
   * Beta parameter
   */
  double beta;

  /**
   * Constructor.
   *
   * @param beta Beta parameter
   */
  public FlexibleBetaLinkage(double beta) {
    this.alpha = 0.5 * (1. - beta);
    this.beta = beta;
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return alpha * dx + alpha * dy + beta * dxy;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Lance-Williams flexible beta parameter.
     */
    public static final OptionID BETA_ID = new OptionID("lancewilliams.beta", "Beta for the Lance-Williams flexible beta approach.");

    /**
     * Beta parameter
     */
    double beta;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(BETA_ID, -0.25)//
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .addConstraint(new GreaterConstraint(-1.)) // Better even < 0
          .grab(config, x -> beta = x);
    }

    @Override
    public FlexibleBetaLinkage make() {
      return new FlexibleBetaLinkage(beta);
    }
  }
}
