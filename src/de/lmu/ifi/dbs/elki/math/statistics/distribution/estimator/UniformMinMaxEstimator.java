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
 * Estimate the uniform distribution by computing min and max.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has UniformDistribution - - estimates
 */
public class UniformMinMaxEstimator implements DistributionEstimator<UniformDistribution> {
  /**
   * The most naive estimator possible: uses minimum and maximum.
   */
  public static final UniformMinMaxEstimator STATIC = new UniformMinMaxEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private UniformMinMaxEstimator() {
    super();
  }

  @Override
  public <A> UniformDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    DoubleMinMax mm = new DoubleMinMax();
    for (int i = 0; i < len; i++) {
      mm.put(adapter.getDouble(data, i));
    }
    return estimate(mm);
  }

  /**
   * Estimate parameters from minimum and maximum observed.
   * 
   * @param mm Minimum and Maximum
   * @return Estimation
   */
  public UniformDistribution estimate(DoubleMinMax mm) {
    return new UniformDistribution(mm.getMin(), mm.getMax());
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
    protected UniformMinMaxEstimator makeInstance() {
      return STATIC;
    }
  }
}
