package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WaldDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate parameter of the Wald distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has WaldDistribution
 */
public class WaldMOMEstimator extends AbstractMeanVarianceEstimator<WaldDistribution> {
  /**
   * Static instance.
   */
  public static final WaldMOMEstimator STATIC = new WaldMOMEstimator();

  @Override
  public WaldDistribution estimateFromMeanVariance(MeanVariance mv) {
    double mean = mv.getMean();
    return new WaldDistribution(mean, mean * mean * mean / mv.getSampleVariance());
  }

  @Override
  public Class<? super WaldDistribution> getDistributionClass() {
    return WaldDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected WaldMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
