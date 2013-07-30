package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExponentialDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Exponential distribution parameters using Median and MAD.
 * 
 * Reference:
 * <p>
 * Robust Estimators for Transformed Location Scale Families<br />
 * D. J. Olive
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ExponentialDistribution
 */
@Reference(title = "Robust Estimators for Transformed Location Scale Families", authors = "D. J. Olive", booktitle = "")
public class ExponentialMedianEstimator extends AbstractMADEstimator<ExponentialDistribution> {
  /**
   * Static instance.
   */
  public static final ExponentialMedianEstimator STATIC = new ExponentialMedianEstimator();

  /**
   * Private constructor, use static instance!
   */
  private ExponentialMedianEstimator() {
    // Do not instantiate
  }

  @Override
  public ExponentialDistribution estimateFromMedianMAD(double median, double mad) {
    return new ExponentialDistribution(1.441 * median);
  }

  @Override
  public Class<? super ExponentialDistribution> getDistributionClass() {
    return ExponentialDistribution.class;
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
    protected ExponentialMedianEstimator makeInstance() {
      return STATIC;
    }
  }
}
