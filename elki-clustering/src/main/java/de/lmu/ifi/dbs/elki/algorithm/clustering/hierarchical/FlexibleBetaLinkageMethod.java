/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Flexible-beta linkage as proposed by Lance and Williams.
 * 
 * Beta values larger than 0 cause chaining, and are thus not recommended.
 * Instead, choose a value between -1 and 0.
 * 
 * Reference:
 * <p>
 * G. N. Lance and W. T. Williams<br />
 * A general theory of classificatory sorting strategies 1. Hierarchical systems
 * <br/>
 * The computer journal 9.4 (1967): 373-380.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "G. N. Lance and W. T. Williams", //
    title = "A general theory of classificatory sorting strategies 1. Hierarchical systems", //
    booktitle = "The computer journal 9.4", //
    url = "http://dx.doi.org/ 10.1093/comjnl/9.4.373")
@Alias({ "flex", "beta" })
public class FlexibleBetaLinkageMethod implements LinkageMethod {
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
  public FlexibleBetaLinkageMethod(double beta) {
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Lance-Williams flexible beta parameter.
     */
    public static final OptionID BETA_ID = new OptionID("lancewilliams.beta", "Beta for the Lance-Williams flexible beta approach.");

    /**
     * Beta parameter
     */
    double beta;

    @Override
    protected void makeOptions(Parameterization config) {
      DoubleParameter betaP = new DoubleParameter(BETA_ID, -0.25)//
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .addConstraint(new GreaterConstraint(-1.)); // Better even < 0
      if(config.grab(betaP)) {
        beta = betaP.doubleValue();
      }
    }

    @Override
    protected FlexibleBetaLinkageMethod makeInstance() {
      return new FlexibleBetaLinkageMethod(beta);
    }
  }
}
