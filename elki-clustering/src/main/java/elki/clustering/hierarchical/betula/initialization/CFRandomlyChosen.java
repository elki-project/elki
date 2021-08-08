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
package elki.clustering.hierarchical.betula.initialization;

import elki.clustering.kmeans.initialization.RandomlyChosen;

import java.util.ArrayList;
import java.util.Random;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.clustering.hierarchical.betula.CFTree;
import elki.utilities.random.RandomFactory;

/**
 * Initialize K-means by randomly choosing k existing elements as initial
 * cluster centers for Clustering Features. For normal k-means use
 * {@link RandomlyChosen}.
 * 
 * @author Andreas Lang
 */
public class CFRandomlyChosen extends AbstractCFKMeansInitialization {
  public CFRandomlyChosen(RandomFactory rf) {
    super(rf);
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, ArrayList<? extends CFInterface> cfs, int k) {
    final int leaves = cfs.size();
    if(leaves < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + leaves + " < k objects.");
    }
    Random rnd = rf.getSingleThreadedRandom();
    int d = cfs.get(0).getDimensionality();
    double[][] means = new double[k][d];
    int remaining = leaves;
    for(int i = 0; i < leaves && k > 0; i++) {
      final CFInterface cfsi = cfs.get(i);
      double prob = rnd.nextDouble();
      if(prob < ((double) k / remaining)) {
        int c = --k;
        for(int j = 0; j < d; j++) {
          means[c][j] = cfsi.centroid(j);
        }
      }
      remaining--;
    }
    return means;
  }

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par extends AbstractCFKMeansInitialization.Par {
    @Override
    public CFRandomlyChosen make() {
      return new CFRandomlyChosen(rnd);
    }
  }
}
