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
 * Probability weighted moments based estimator.
 * 
 * @author Jonathan von Brünken
 * @author Erich Schubert
 */
public class PWMEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final PWMEstimator STATIC = new PWMEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter, final int len) {
    if(len == 2) { // Fallback to MoM
      double v1 = adapter.getDouble(data, 0) / adapter.getDouble(data, 1);
      return v1 / (1 - v1);
    }
    final int num = len - 1; // Except for last
    // Estimate first PWM (k=1), using plotting position i/(n-1):
    double v1 = 0.;
    for(int i = 0; i < num; i++) {
      // TODO: by the Landwehr formula, we use the first data point!
      v1 += adapter.getDouble(data, i) * i;
    }
    // All scaling factors collected for performance reasons:
    final double w = adapter.getDouble(data, num);
    v1 /= num * w * (num - 1);
    return v1 / (1 - 2 * v1);
  }
}
