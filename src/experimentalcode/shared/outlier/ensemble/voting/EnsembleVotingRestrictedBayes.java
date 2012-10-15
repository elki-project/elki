package experimentalcode.shared.outlier.ensemble.voting;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Combination rule based on Bayes theorems.
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingRestrictedBayes implements EnsembleVoting {
  /**
   * Option ID for the minimum and maximum vote
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("ensemble.bayes.min", "Minimum vote share.");

  /**
   * Minimum vote to cast.
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, 0.05, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 1.0, IntervalConstraint.IntervalBoundary.OPEN));

  /**
   * Option ID for the minimum and maximum vote
   */
  public static final OptionID MAX_ID = OptionID.getOrCreateOptionID("ensemble.bayes.max", "Maximum vote share.");

  /**
   * Minimum vote to cast.
   */
  private final DoubleParameter MAX_PARAM = new DoubleParameter(MAX_ID, 0.95, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.CLOSE));

  /**
   * Minimum vote to cast
   */
  private double minvote = 0.05;

  /**
   * Maximum vote to cast
   */
  private double maxvote = 1.0;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EnsembleVotingRestrictedBayes(Parameterization config) {
    config = config.descend(this);
    if(config.grab(MIN_PARAM)) {
      minvote = MIN_PARAM.getValue();
    }
    if(config.grab(MAX_PARAM)) {
      maxvote = MAX_PARAM.getValue();
    }
  }

  @Override
  public double combine(List<Double> scores) {
    double pos = 1.0;
    double neg = 1.0;
    for(Double score : scores) {
      final double cscore = minvote + score * (maxvote - minvote);
      pos *= cscore;
      neg *= (1.0 - cscore);
    }
    return pos / (pos + neg);
  }
}