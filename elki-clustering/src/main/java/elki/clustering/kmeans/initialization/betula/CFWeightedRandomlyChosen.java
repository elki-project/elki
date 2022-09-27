/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.clustering.kmeans.initialization.betula;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.documentation.Reference;
import elki.utilities.random.RandomFactory;

/**
 * Initialize K-means by randomly choosing k existing elements as initial
 * cluster centers for Clustering Features. For normal k-means use
 * {@link RandomlyChosen}. This version uses the number of points in each
 * cluster feature for weighting.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class CFWeightedRandomlyChosen extends AbstractCFKMeansInitialization {
  /**
   * Constructor.
   *
   * @param rf Random generator
   */
  public CFWeightedRandomlyChosen(RandomFactory rf) {
    super(rf);
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends ClusterFeature> cfs, int k) {
    final int leaves = cfs.size();
    if(leaves < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + leaves + " < k objects.");
    }
    Random rnd = rf.getSingleThreadedRandom();
    double[][] means = new double[k][];
    int weightsum = tree.getRoot().getCF().getWeight();
    ArrayList<ClusterFeature> cpy = new ArrayList<>(cfs);
    for(int i = 0; i < k; i++) {
      int weightpos = rnd.nextInt(weightsum);
      int off = 0;
      while((weightpos -= cpy.get(off).getWeight()) >= 0) {
        off++;
      }
      means[i] = cpy.get(off).toArray();
      weightsum -= cpy.get(off).getWeight();
      cpy.set(off, cpy.get(cpy.size() - i - 1));
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
    public CFWeightedRandomlyChosen make() {
      return new CFWeightedRandomlyChosen(rnd);
    }
  }
}
