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

import elki.index.tree.betula.CFNode;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.random.RandomFactory;

/**
 * Trunk strategy for initializing k-means with BETULA: only the nodes up to a
 * particular level are considered for k-means++ style initialization.
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
@Alias("trunk")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class CFKPlusPlusTrunk extends CFKPlusPlusLeaves {
  /**
   * Constructor.
   * 
   * @param dist distance function
   * @param firstUniform choose the first center uniformly from leaves
   * @param rf random generator
   */
  public CFKPlusPlusTrunk(CFInitWeight dist, boolean firstUniform, RandomFactory rf) {
    super(dist, firstUniform, rf);
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends ClusterFeature> cfs, int k) {
    if(tree.numLeaves() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + tree.numLeaves() + " < k objects.");
    }
    List<ClusterFeature> current = new ArrayList<>((k * tree.getCapacity()) >> 1);
    List<CFNode<?>> todo = new ArrayList<>(1);
    todo.add(tree.getRoot());
    while(todo.size() + current.size() < k && !todo.isEmpty()) {
      List<CFNode<?>> prev = todo;
      todo = new ArrayList<>(prev.size() * tree.getCapacity());
      for(int j = 0; j < prev.size(); j++) {
        CFNode<?> node = prev.get(j);
        for(int i = 0; i < node.capacity(); i++) {
          AsClusterFeature child = node.getChild(i);
          if(child == null) {
            break;
          }
          if(child instanceof CFNode) {
            todo.add((CFNode<?>) child);
          }
          else {
            current.add(child.getCF());
          }
        }
      }
    }
    // Add the remaining (unexpanded) nodes, too:
    for(CFNode<?> node : todo) {
      current.add(node.getCF());
    }
    // We know that the super class ignores the tree, and only uses cfs.
    return super.chooseInitialMeans(tree, current, k);
  }

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par extends CFKPlusPlusLeaves.Par {
    @Override
    public void configure(Parameterization config) {
      super.configure(config);
    }

    @Override
    public CFKPlusPlusTrunk make() {
      return new CFKPlusPlusTrunk(dist, firstUniform, rnd);
    }
  }
}
