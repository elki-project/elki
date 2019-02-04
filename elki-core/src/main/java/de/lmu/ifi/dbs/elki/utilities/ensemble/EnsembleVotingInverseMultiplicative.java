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
package de.lmu.ifi.dbs.elki.utilities.ensemble;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Inverse multiplicative voting:
 * \( 1-\prod_i(1-s_i) \)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class EnsembleVotingInverseMultiplicative implements EnsembleVoting {
  /**
   * Static instance.
   */
  public static final EnsembleVotingInverseMultiplicative STATIC = new EnsembleVotingInverseMultiplicative();

  /**
   * Constructor.
   */
  public EnsembleVotingInverseMultiplicative() {
    super();
  }

  @Override
  public double combine(double[] scores) {
    return combine(scores, scores.length);
  }

  @Override
  public double combine(double[] scores, int count) {
    double prod = 1.;
    for(int i = 0; i < count; i++) {
      prod *= (1 - scores[i]);
    }
    return 1 - prod;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected EnsembleVotingInverseMultiplicative makeInstance() {
      return STATIC;
    }
  }
}
