/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
 * ELKI Development Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.index.tree.betula.initialization;

import elki.data.NumberVector;
import elki.index.tree.betula.features.ClusterFeature;

/**
 * Calculates the Euclidean distance on any of the BETULA clustering Features.
 *
 * @author Andreas Lang
 */
public class EuclideanDistance implements CFIDistance {
  @Override
  public double squaredDistance(NumberVector v, ClusterFeature cf) {
    final int d = v.getDimensionality();
    assert d == cf.getDimensionality();
    double sum = 0.;
    for(int i = 0; i < d; i++) {
      double dx = cf.centroid(i) - v.doubleValue(i);
      sum += dx * dx;
    }
    return sum;
  }

  @Override
  public double squaredDistance(double[] v, ClusterFeature cf) {
    final int d = v.length;
    assert d == cf.getDimensionality();
    double sum = 0.;
    for(int i = 0; i < d; i++) {
      double dx = cf.centroid(i) - v[i];
      sum += dx * dx;
    }
    return sum;
  }

  @Override
  public double squaredDistance(ClusterFeature c1, ClusterFeature c2) {
    final int d = c1.getDimensionality();
    assert (d == c2.getDimensionality());
    double sum = 0.;
    for(int i = 0; i < d; i++) {
      double dx = c1.centroid(i) - c2.centroid(i);
      sum += dx * dx;
    }
    return sum;
  }
}
