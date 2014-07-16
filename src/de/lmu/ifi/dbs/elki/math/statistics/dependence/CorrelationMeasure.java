package de.lmu.ifi.dbs.elki.math.statistics.dependence;

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
 * Pearson product-moment correlation coefficient.
 * 
 * @author Erich Schubert
 */
public class CorrelationMeasure extends AbstractDependenceMeasure {
  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    // Perform two-pass estimation, which is numerically stable and often faster
    // than the Knuth-Welford approach (see PearsonCorrelation class)
    double m1 = 0., m2 = 0;
    for(int i = 0; i < len; i++) {
      m1 += adapter1.getDouble(data1, i);
      m2 += adapter2.getDouble(data2, i);
    }
    m1 /= len;
    m2 /= len;
    // Second pass: variances and covariance
    double v1 = 0., v2 = 0., cov = 0.;
    for(int i = 0; i < len; i++) {
      double d1 = adapter1.getDouble(data1, i) - m1;
      double d2 = adapter2.getDouble(data2, i) - m2;
      v1 += d1 * d1;
      v2 += d2 * d2;
      cov += d1 * d2;
    }
    return cov / Math.sqrt(v1 * v2);
  }
}
