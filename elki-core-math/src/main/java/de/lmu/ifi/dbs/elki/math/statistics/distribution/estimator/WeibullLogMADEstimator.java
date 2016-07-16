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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Parameter estimation via median and median absolute deviation from median
 * (MAD).
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
 * @apiviz.has WeibullDistribution - - estimates
 */
@Reference(title = "Applied Robust Statistics", authors = "D. J. Olive", booktitle = "Applied Robust Statistics", url="http://lagrange.math.siu.edu/Olive/preprints.htm")
public class WeibullLogMADEstimator extends AbstractLogMADEstimator<WeibullDistribution> {
  /**
   * The more robust median based estimator.
   */
  public static final WeibullLogMADEstimator STATIC = new WeibullLogMADEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private WeibullLogMADEstimator() {
    super();
  }

  @Override
  public WeibullDistribution estimateFromLogMedianMAD(double median, double mad, double shift) {
    double isigma = 1.30370 / mad;
    double lambda = Math.exp(isigma * median - MathUtil.LOGLOG2);

    return new WeibullDistribution(isigma, lambda);
  }

  @Override
  public Class<? super WeibullDistribution> getDistributionClass() {
    return WeibullDistribution.class;
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
    protected WeibullLogMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
