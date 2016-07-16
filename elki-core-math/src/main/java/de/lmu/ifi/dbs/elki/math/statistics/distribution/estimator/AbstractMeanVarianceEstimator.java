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
import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Estimators that work on Mean and Variance only (i.e. the first two moments
 * only).
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <D> Distribution to estimate.
 */
public abstract class AbstractMeanVarianceEstimator<D extends Distribution> extends AbstractMOMEstimator<D> implements MeanVarianceDistributionEstimator<D> {
  /**
   * Constructor.
   */
  public AbstractMeanVarianceEstimator() {
    super();
  }

  @Override
  public D estimateFromStatisticalMoments(StatisticalMoments moments) {
    return estimateFromMeanVariance(moments);
  }

  @Override
  public abstract D estimateFromMeanVariance(MeanVariance mv);

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    MeanVariance mv = new MeanVariance();
    int size = adapter.size(data);
    for (int i = 0; i < size; i++) {
      final double val = adapter.getDouble(data, i);
      if (Double.isInfinite(val) || Double.isNaN(val)) {
        continue;
      }
      mv.put(val);
    }
    return estimateFromMeanVariance(mv);
  }
}
