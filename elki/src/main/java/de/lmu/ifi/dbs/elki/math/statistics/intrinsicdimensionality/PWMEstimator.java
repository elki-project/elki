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
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int n = adapter.size(data);
    // Estimate first PWM (k=1), ignoring the last value:
    double v1 = 0.;
    final int num = n - 1;
    for(int i = 0; i < num; i++) {
      // Note that our i starts at 0, i + 1 - 0.35 = i + 0.65
      v1 += adapter.getDouble(data, i) * (i + 0.65);
    }
    v1 /= num * num;
    final double w = adapter.getDouble(data, n - 1);
    v1 /= w;
    return v1 / (1 - v1 * 2);
  }
}
