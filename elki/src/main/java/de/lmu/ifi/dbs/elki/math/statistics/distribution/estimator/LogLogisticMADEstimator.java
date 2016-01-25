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
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogLogisticDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Logistic distribution parameters using Median and MAD.
 * 
 * Reference:
 * <p>
 * D. J. Olive<br />
 * Applied Robust Statistics<br />
 * Preprint of an upcoming book, University of Minnesota
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has LogLogisticDistribution
 */
@Reference(title = "Applied Robust Statistics", authors = "D. J. Olive", booktitle = "Applied Robust Statistics", url="http://lagrange.math.siu.edu/Olive/preprints.htm")
public class LogLogisticMADEstimator extends AbstractMADEstimator<LogLogisticDistribution> {
  /**
   * Static instance.
   */
  public static final LogLogisticMADEstimator STATIC = new LogLogisticMADEstimator();

  /**
   * Private constructor, use static instance!
   */
  private LogLogisticMADEstimator() {
    // Do not instantiate
  }

  @Override
  public LogLogisticDistribution estimateFromMedianMAD(double median, double mad) {
    return new LogLogisticDistribution(1. / median, MathUtil.LOG3 / mad);
  }

  @Override
  public Class<? super LogLogisticDistribution> getDistributionClass() {
    return LogLogisticDistribution.class;
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
    protected LogLogisticMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
