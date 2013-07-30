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
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Slightly improved estimation, that takes sample size into account and
 * enhances the interval appropriately.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has UniformDistribution - - estimates
 */
public class UniformEnhancedMinMaxEstimator implements DistributionEstimator<UniformDistribution> {
  /**
   * Slightly more refined estimator: takes sample size into account.
   */
  public static final UniformEnhancedMinMaxEstimator STATIC = new UniformEnhancedMinMaxEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private UniformEnhancedMinMaxEstimator() {
    super();
  }

  @Override
  public <A> UniformDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    DoubleMinMax mm = new DoubleMinMax();
    for (int i = 0; i < len; i++) {
      mm.put(adapter.getDouble(data, i));
    }
    double grow = (len > 1) ? 0.5 * mm.getDiff() / (len - 1) : 0.;
    return new UniformDistribution(mm.getMin() - grow, mm.getMax() + grow);
  }

  @Override
  public Class<? super UniformDistribution> getDistributionClass() {
    return UniformDistribution.class;
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
    protected UniformEnhancedMinMaxEstimator makeInstance() {
      return STATIC;
    }
  }
}
