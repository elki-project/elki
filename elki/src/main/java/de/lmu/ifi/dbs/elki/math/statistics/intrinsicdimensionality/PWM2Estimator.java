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
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Probability weighted moments based estimator, using the second moment.
 * 
 * It can be shown theoretically that this estimator is expected to have a
 * higher variance than the one using the first moment only, it is included for
 * completeness only.
 * 
 * @author Erich Schubert
 */
public class PWM2Estimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final PWM2Estimator STATIC = new PWM2Estimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter, final int len) {
    if(len == 2) { // Fallback to MoM
      double v1 = adapter.getDouble(data, 0) / adapter.getDouble(data, 1);
      return v1 / (1 - v1);
    }
    if(len == 3) { // Fallback to first moment only
      double v1 = adapter.getDouble(data, 1) * .5 / adapter.getDouble(data, 2);
      return v1 / (1 - 2 * v1);
    }
    final int num = len - 1; // Except for last
    // Estimate second PWM (k=2), using plotting position i/(n-1):
    double v2 = 0.;
    // TODO: by the Landwehr formula, we lose the first two data points:
    for(int i = 2; i < num; i++) {
      v2 += adapter.getDouble(data, i) * i * (i - 1);
    }
    // All scaling factors collected for performance reasons
    final double w = adapter.getDouble(data, num);
    v2 /= num * w * (num - 1.) * (num - 2.);
    return v2 / (1 - 3 * v2);
  }
}
