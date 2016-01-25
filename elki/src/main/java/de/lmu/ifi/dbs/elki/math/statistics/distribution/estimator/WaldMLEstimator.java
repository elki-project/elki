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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WaldDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate parameter of the Wald distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has WaldDistribution
 */
public class WaldMLEstimator implements DistributionEstimator<WaldDistribution> {
  /**
   * Static instance.
   */
  public static final WaldMLEstimator STATIC = new WaldMLEstimator();

  @Override
  public <A> WaldDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double mean = 0.;
    for(int i = 0; i < len; i++) {
      double v = adapter.getDouble(data, i);
      mean += v;
    }
    mean /= len;
    double invmean = 1. / mean;
    double invdev = 0.;
    for(int i = 0; i < len; i++) {
      double v = adapter.getDouble(data, i);
      if(v > 0.) {
        invdev += 1. / v - invmean;
      }
    }
    return new WaldDistribution(mean, len / invdev);
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
    protected WaldMLEstimator makeInstance() {
      return STATIC;
    }
  }
}
