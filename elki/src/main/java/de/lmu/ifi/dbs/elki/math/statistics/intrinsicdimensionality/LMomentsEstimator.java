package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Probability weighted moments based estimator using L-Moments.
 * 
 * Derived from the L-Moments estimation for the exponential distribution.
 * 
 * @author Jonathan von Brünken
 * @author Erich Schubert
 */
public class LMomentsEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final LMomentsEstimator STATIC = new LMomentsEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter, final int len) {
    final double w = adapter.getDouble(data, len - 1);
    double[] excess = new double[len - 1];
    for(int i = 0, j = len - 2; j >= 0; ++i, --j) {
      excess[i] = adapter.getDouble(data, j);
    }
    double[] lmom = ProbabilityWeightedMoments.samLMR(excess, ArrayLikeUtil.doubleArrayAdapter(), 2);
    if(lmom[1] == 0) {
      return -.5 * (lmom[0] * 2) / w; // Fallback to first moment only.
    }
    return -.5 * ((lmom[0] * lmom[0] / lmom[1]) - lmom[0]) / w;
  }
}
