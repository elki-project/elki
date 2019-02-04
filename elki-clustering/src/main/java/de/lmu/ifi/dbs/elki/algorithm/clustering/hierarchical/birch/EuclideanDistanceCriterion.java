/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance criterion.
 * 
 * This is not found in the original work, but used in many implementation
 * attempts: assign points if the Euclidean distances is below a threshold.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class EuclideanDistanceCriterion implements BIRCHAbsorptionCriterion {
  /**
   * Static instance.
   */
  public static final EuclideanDistanceCriterion STATIC = new EuclideanDistanceCriterion();

  @Override
  public double squaredCriterion(ClusteringFeature f1, NumberVector n) {
    assert (f1.n > 0);
    final int dim = f1.ls.length;
    final double div = 1. / f1.n;
    double sum = 0;
    for(int d = 0; d < dim; d++) {
      double v = f1.ls[d] * div - n.doubleValue(d);
      sum += v * v;
    }
    return sum;
  }

  @Override
  public double squaredCriterion(ClusteringFeature f1, ClusteringFeature f2) {
    assert (f1.n > 0 && f2.n > 0);
    final int dim = f1.ls.length;
    final double div1 = 1. / f1.n, div2 = 1. / f2.n;
    double sum = 0;
    for(int d = 0; d < dim; d++) {
      double v = f1.ls[d] * div1 - f2.ls[d] * div2;
      sum += v * v;
    }
    return sum;
  }

  /**
   * Parameterization class
   *
   * @hidden
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected EuclideanDistanceCriterion makeInstance() {
      return STATIC;
    }
  }
}
