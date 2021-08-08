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
import java.util.Arrays;
import java.util.Random;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.clustering.hierarchical.betula.CFNode;
import elki.clustering.hierarchical.betula.CFTree;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Initialize K-means by choosing k distant existing subtrees.
 *
 * @author Andreas Lang
 */
public class CFKppTree extends AbstractCFKMeansInitialization {
  int maxdepth = -1;

  CFIDistance dist;

  boolean first_var;

  public CFKppTree(RandomFactory rf, CFIDistance dist, int maxdepth, boolean first_var) {
    super(rf);
    this.dist = dist;
    this.maxdepth = maxdepth;
    this.first_var = first_var;
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, ArrayList<? extends CFInterface> cfs, int k) {
    if(maxdepth == 0) {
      maxdepth = FastMath.log2(k) / FastMath.log2(tree.getCapacity()) + 1;
    }
    if(tree.getLeaves() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + tree.getLeaves() + " < k objects.");
    }
    Random rnd = rf.getSingleThreadedRandom();
    ArrayList<CFInterface> ccs = new ArrayList<>(k);
    if(first_var) {
      ccs.set(0, chooseFirstNode(tree.getRoot(), cfs, rnd));
    }
    else {
      // TODO: use weights for choosing the initial center!
      ccs.set(0, (CFInterface) cfs.get(rnd.nextInt(cfs.size())));
    }
    for(int m = 1; m < k; m++) {
      CFInterface next = tree.getRoot();
      int depth = maxdepth;
      while(next instanceof CFNode && depth != 0) {
        depth--;
        @SuppressWarnings("unchecked")
        CFNode<CFInterface> current = (CFNode<CFInterface>) next;
        next = chooseNextNode(current, ccs, m, rnd);
      }
      ccs.add(next);
    }
    // Convert CFs to means:
    final int d = ccs.get(0).getDimensionality();
    double[][] means = new double[k][d];
    for(int i = 0; i < k; i++) {
      final CFInterface ccsi = ccs.get(i);
      double[] mean = means[i];
      for(int j = 0; j < d; j++) {
        mean[j] = ccsi.centroid(j);
      }
    }
    return means;
  }

  private CFInterface chooseFirstNode(CFInterface center, ArrayList<? extends CFInterface> cfs, Random rnd) {
    double weightsum = 0;
    double[] weights = new double[cfs.size()];
    Arrays.fill(weights, Double.POSITIVE_INFINITY);
    int i = 0;
    while(true) {
      final CFInterface child = cfs.get(i);
      if(child == null) {
        break;
      }
      double weight = dist.squaredDistance(center, child);
      if(weight < weights[i]) {
        weights[i] = weight;
      }
      weightsum += weights[i];
      i++;
    }

    while(true) {
      double r = rnd.nextDouble() * weightsum;
      for(int j = 0; j < i; j++) {
        if((r -= weights[j]) <= 0) {
          return cfs.get(j);
        }
      }
      weightsum -= r; // Decrease
    }
  }

  private CFInterface chooseNextNode(CFNode<?> current, ArrayList<? extends CFInterface> ccs, int m, Random rnd) {
    double weightsum = 0;
    double[] weights = new double[current.capacity()];
    Arrays.fill(weights, Double.POSITIVE_INFINITY);
    int i = 0;
    while(true) {
      final CFInterface child = current.getChild(i);
      if(child == null) {
        break;
      }
      double weight = 0;
      for(int j = 0; j < m; j++) {
        weight = dist.squaredDistance(ccs.get(j), child);
        if(weight < weights[i]) {
          weights[i] = weight;
        }
      }
      weightsum += weights[i];
      i++;
    }

    while(true) {
      double r = rnd.nextDouble() * weightsum;
      for(int j = 0; j < i; j++) {
        if((r -= weights[j]) <= 0) {
          return current.getChild(j);
        }
      }
      weightsum -= r; // Decrease
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par extends AbstractCFKMeansInitialization.Par {
    public static final OptionID DEPTH_ID = new OptionID("kmpp.depth", //
        "maximum depth for intitialization");

    /**
     * k Means distance.
     */
    public static final OptionID KMPlusPlus_ID = new OptionID("kmeans.distance", "Distance to use for kmeans++ criterion");

    public static final OptionID KMPFirst_ID = new OptionID("kmpp.first_var", "Chooose first dependent on Var");

    int depth = -1;

    CFIDistance dist;

    boolean first_var = false;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
      new ObjectParameter<CFIDistance>(KMPlusPlus_ID, CFIDistance.class, VarDist.class)//
          .grab(config, x -> dist = x);
      new Flag(KMPFirst_ID).grab(config, x -> first_var = x);
      new IntParameter(DEPTH_ID, -1)//
          .grab(config, x -> depth = x);
    }

    @Override
    public CFKppTree make() {
      return new CFKppTree(rnd, dist, depth, first_var);
    }
  }
}
