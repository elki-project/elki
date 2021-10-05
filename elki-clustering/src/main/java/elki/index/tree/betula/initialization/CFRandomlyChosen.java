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

import java.util.List;
import java.util.Random;

import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.random.RandomFactory;

/**
 * Initialize K-means by randomly choosing k existing elements as initial
 * cluster centers for Clustering Features. For normal k-means use
 * {@link RandomlyChosen}.
 * 
 * @author Andreas Lang
 */
public class CFRandomlyChosen extends AbstractCFKMeansInitialization {
  /**
   * Constructor.
   *
   * @param rf Random generator
   */
  public CFRandomlyChosen(RandomFactory rf) {
    super(rf);
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends AsClusterFeature> cfs, int k) {
    final int leaves = cfs.size();
    if(leaves < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + leaves + " < k objects.");
    }
    Random rnd = rf.getSingleThreadedRandom();
    int d = cfs.get(0).getCF().getDimensionality();
    double[][] means = new double[k][d];
    for(int i = 0, c = 0; i < leaves && k > 0; i++) {
      final ClusterFeature cfsi = cfs.get(i).getCF();
      double prob = rnd.nextDouble();
      if(prob < ((double) k / (leaves - i))) {
        double[] mean = means[c++];
        for(int j = 0; j < d; j++) {
          mean[j] = cfsi.centroid(j);
        }
      }
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
