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

import java.util.ArrayList;
import java.util.List;

import elki.clustering.hierarchical.betula.CFNode;
import elki.clustering.hierarchical.betula.CFTree;
import elki.clustering.hierarchical.betula.features.ClusterFeature;
import elki.clustering.hierarchical.betula.features.AsClusterFeature;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Initialize K-means by choosing k distant existing subtrees.
 *
 * @author Andreas Lang
 */
public class CFTreeInit extends AbstractCFKMeansInitialization {
  AbstractCFKMeansInitialization subInit;

  public CFTreeInit(RandomFactory rf, AbstractCFKMeansInitialization subInit) {
    super(rf);
    this.subInit = subInit;
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends AsClusterFeature> cfs, int k) {
    if(tree.getLeaves() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + tree.getLeaves() + " < k objects.");
    }
    ArrayList<CFNode<?>> next = new ArrayList<>();
    ArrayList<ClusterFeature> res = new ArrayList<>(k);
    next.add(tree.getRoot());
    do {
      ArrayList<CFNode<?>> current = next;
      next = new ArrayList<>();
      while(current.size() > 0) {
        CFNode<?> node = current.remove(0);
        for(int i = 0; i < node.capacity(); i++) {
          AsClusterFeature child = node.getChild(i);
          if(child == null) {
            break;
          }
          if(child instanceof CFNode) {
            next.add((CFNode<?>) child);
          }
          else {
            res.add((ClusterFeature) child);
          }
        }
      }
    }
    while(res.size() + next.size() < k);
    for(CFNode<?> cn : next) {
      res.add(cn.getCF());
    }
    return subInit.chooseInitialMeans(tree, res, k);
  }

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par extends AbstractCFKMeansInitialization.Par {
    /**
     * k Means distance.
     */
    public static final OptionID TIKMPlusPlus_ID = new OptionID("Tree.init.centers", "Which method should be used for initialitzation");

    /**
     * Initialization method
     */
    protected AbstractCFKMeansInitialization initializer;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
      new ObjectParameter<AbstractCFKMeansInitialization>(TIKMPlusPlus_ID, AbstractCFKMeansInitialization.class, CFKMeansPlusPlus.class) //
          .grab(config, x -> initializer = x);
    }

    @Override
    public CFTreeInit make() {
      return new CFTreeInit(rnd, initializer);
    }
  }
}
