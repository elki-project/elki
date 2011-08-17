package experimentalcode.shared.outlier.ensemble.voting;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
public class EnsembleVotingBayes implements EnsembleVoting {
  /**
   * Option ID for the minimum and maximum vote
   */
  public final static OptionID MIN_ID = OptionID.getOrCreateOptionID("ensemble.bayes.min", "Minimum (and maximum) vote share.");

  /**
   * Minimum vote to cast.
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 0.5, IntervalConstraint.IntervalBoundary.OPEN), 0.05);

  /**
   * Minimum vote to cast
   */
  private double minvote = 0.05;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EnsembleVotingBayes(Parameterization config) {
    config = config.descend(this);
    if(config.grab(MIN_PARAM)) {
      minvote = MIN_PARAM.getValue();
    }
  }

  @Override
  public double combine(List<Double> scores) {
    double pos = 1.0;
    double neg = 1.0;
    for(Double score : scores) {
      if(score < minvote) {
        score = minvote;
      }
      else if(score > 1.0 - minvote) {
        score = 1.0 - minvote;
      }
      pos *= score;
      neg *= (1.0 - score);
    }
    return pos / (pos + neg);
  }
}