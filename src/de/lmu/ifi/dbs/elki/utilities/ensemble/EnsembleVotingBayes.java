package de.lmu.ifi.dbs.elki.utilities.ensemble;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Combination rule based on Bayes theorems.
 * 
 * Note: this assumes that the scores are probabilistic!
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingBayes implements EnsembleVoting {
  /**
   * Minimum vote to cast
   */
  private double minvote = 0.05;

  /**
   * Constructor.
   * 
   * @param minvote Minimum vote to cast (0 to 0.5)
   */
  public EnsembleVotingBayes(double minvote) {
    this.minvote = minvote;
  }

  @Override
  public double combine(double[] scores) {
    double pos = 1.0;
    double neg = 1.0;
    for (double score : scores) {
      if (score < minvote) {
        score = minvote;
      } else if (score > 1.0 - minvote) {
        score = 1.0 - minvote;
      }
      pos *= score;
      neg *= (1.0 - score);
    }
    return pos / (pos + neg);
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
     * Option ID for the minimum and maximum vote
     */
    public static final OptionID MIN_ID = new OptionID("ensemble.bayes.min", "Minimum (and maximum) vote share, in the range 0 to 0.5");

    /**
     * Minimum vote to cast
     */
    private double minvote = 0.05;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minvoteP = new DoubleParameter(MIN_ID, 0.05);
      minvoteP.addConstraint(new GreaterEqualConstraint(0.0));
      minvoteP.addConstraint(new LessConstraint(0.5));

      if (config.grab(minvoteP)) {
        minvote = minvoteP.getValue();
      }
    }

    @Override
    protected EnsembleVotingBayes makeInstance() {
      return new EnsembleVotingBayes(minvote);
    }
  }
}
