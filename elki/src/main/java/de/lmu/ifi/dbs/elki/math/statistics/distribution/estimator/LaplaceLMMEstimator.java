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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LaplaceDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Laplace distribution parameters using the method of L-Moments (LMM).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has LaplaceDistribution
 */
public class LaplaceLMMEstimator extends AbstractLMMEstimator<LaplaceDistribution> {
  /**
   * Static instance.
   */
  public static final LaplaceLMMEstimator STATIC = new LaplaceLMMEstimator();

  /**
   * Private constructor, use static instance!
   */
  private LaplaceLMMEstimator() {
    // Do not instantiate
  }

  @Override
  public LaplaceDistribution estimateFromLMoments(double[] xmom) {
    final double location = xmom[0];
    final double scale = 4. / 3. * xmom[1];
    return new LaplaceDistribution(1. / scale, location);
  }

  @Override
  public int getNumMoments() {
    return 2;
  }

  @Override
  public Class<? super LaplaceDistribution> getDistributionClass() {
    return LaplaceDistribution.class;
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
    protected LaplaceLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
