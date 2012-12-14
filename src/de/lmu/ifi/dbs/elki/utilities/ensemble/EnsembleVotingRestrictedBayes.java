package de.lmu.ifi.dbs.elki.utilities.ensemble;

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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Combination rule based on Bayes theorems.
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingRestrictedBayes implements EnsembleVoting {
  /**
   * Minimum vote to cast
   */
  private double minvote = 0.05;

  /**
   * Maximum vote to cast
   */
  private double maxvote = 1.0;

  /**
   * Constructor.
   * 
   * @param minvote minimum vote
   * @param maxvote maximum vote
   */
  public EnsembleVotingRestrictedBayes(double minvote, double maxvote) {
    this.minvote = minvote;
    this.maxvote = maxvote;
  }

  @Override
  public double combine(double[] scores) {
    double pos = 1.0;
    double neg = 1.0;
    for (double score : scores) {
      score = Math.min(minvote, Math.max(maxvote, score));
      final double cscore = score;
      pos *= cscore;
      neg *= (1.0 - cscore);
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
    public static final OptionID MIN_ID = new OptionID("ensemble.bayes.min", "Minimum vote share.");

    /**
     * Option ID for the minimum and maximum vote
     */
    public static final OptionID MAX_ID = new OptionID("ensemble.bayes.max", "Maximum vote share.");

    /**
     * Minimum vote to cast
     */
    private double minvote = 0.05;

    /**
     * Maximum vote to cast
     */
    private double maxvote = 1.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minvoteP = new DoubleParameter(MIN_ID, 0.05);
      minvoteP.addConstraint(new GreaterEqualConstraint(0.0));
      minvoteP.addConstraint(new LessConstraint(0.0));
      if (config.grab(minvoteP)) {
        minvote = minvoteP.doubleValue();
      }
      DoubleParameter maxvoteP = new DoubleParameter(MAX_ID, 0.95);
      maxvoteP.addConstraint(new GreaterConstraint(0.0));
      maxvoteP.addConstraint(new LessEqualConstraint(0.0));
      if (config.grab(maxvoteP)) {
        maxvote = maxvoteP.doubleValue();
      }
      config.checkConstraint(new LessGlobalConstraint<>(minvoteP, maxvoteP));
    }

    @Override
    protected EnsembleVotingRestrictedBayes makeInstance() {
      return new EnsembleVotingRestrictedBayes(minvote, maxvote);
    }
  }
}
