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

package elki.clustering.kmeans.initialization.betula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import elki.index.tree.betula.CFNode;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Initialize K-means by choosing k distant existing subtrees.
 *
 * @author Andreas Lang
 */
public class CFKppTree extends AbstractCFKMeansInitialization {
  /**
   * Distance function to use for initial means
   */
  CFIDistance dist = null;

  /**
   * Choose the first center based on variance contribution.
   */
  boolean firstVar = false;

  int maxdepth = -1;

  public CFKppTree(RandomFactory rf, CFIDistance dist, int maxdepth, boolean firstVar) {
    super(rf);
    this.dist = dist;
    this.maxdepth = maxdepth;
    this.firstVar = firstVar;
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends AsClusterFeature> cfs, int k) {
    if(tree.numLeaves() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + tree.numLeaves() + " < k objects.");
    }
    maxdepth = maxdepth > 0 ? maxdepth : FastMath.log2(k) / FastMath.log2(tree.getCapacity()) + 1;
    Random rnd = rf.getSingleThreadedRandom();
    List<ClusterFeature> ccs = new ArrayList<>(k);
    ccs.add(firstVar ? chooseFirstNode(tree.getRoot().getCF(), cfs, rnd) :
    // TODO: use weights for choosing the initial center!
        cfs.get(rnd.nextInt(cfs.size())).getCF());
    for(int m = 1; m < k; m++) {
      ClusterFeature next = tree.getRoot().getCF();
      for(int depth = maxdepth; next instanceof CFNode && depth > 0; --depth) {
        next = chooseNextNode((CFNode<?>) next, ccs, m, rnd);
      }
      ccs.add(next);
    }
    // Convert CFs to means:
    final int d = ccs.get(0).getDimensionality();
    double[][] means = new double[k][d];
    for(int i = 0; i < k; i++) {
      final ClusterFeature ccsi = ccs.get(i);
      double[] mean = means[i];
      for(int j = 0; j < d; j++) {
        mean[j] = ccsi.centroid(j);
      }
    }
    return means;
  }

  private ClusterFeature chooseFirstNode(ClusterFeature center, List<? extends AsClusterFeature> cfs, Random rnd) {
    double weightsum = 0;
    double[] weights = new double[cfs.size()];
    Arrays.fill(weights, Double.POSITIVE_INFINITY);
    int i = 0;
    while(true) {
      final ClusterFeature child = cfs.get(i).getCF();
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
          return cfs.get(j).getCF();
        }
      }
      weightsum -= r; // Decrease
    }
  }

  private ClusterFeature chooseNextNode(CFNode<?> current, List<? extends AsClusterFeature> ccs, int m, Random rnd) {
    double weightsum = 0;
    double[] weights = new double[current.capacity()];
    Arrays.fill(weights, Double.POSITIVE_INFINITY);
    int i = 0;
    while(true) {
      final ClusterFeature child = current.getChild(i).getCF();
      if(child == null) {
        break;
      }
      double weight = 0;
      for(int j = 0; j < m; j++) {
        weight = dist.squaredDistance(ccs.get(j).getCF(), child);
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
          return current.getChild(j).getCF();
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
    /**
     * Depth of the trunk based stragegy.
     */
    public static final OptionID DEPTH_ID = new OptionID("kmpp.depth", "maximum depth for intitialization");

    /**
     * k Means distance.
     */
    public static final OptionID KMPP_DISTANCE_ID = CFKMeansPlusPlus.Par.KMPP_DISTANCE_ID;

    /**
     * Choose the first center based on variance contribution.
     */
    public static final OptionID FIRST_VARIANCE_ID = CFKMeansPlusPlus.Par.FIRST_VARIANCE_ID;

    /**
     * Distance function to use for initial means
     */
    CFIDistance dist = null;

    /**
     * Choose the first center based on variance contribution.
     */
    boolean firstVar = false;

    int depth = -1;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<CFIDistance>(KMPP_DISTANCE_ID, CFIDistance.class, VarDistance.class)//
          .grab(config, x -> dist = x);
      new Flag(FIRST_VARIANCE_ID).grab(config, x -> firstVar = x);
      new IntParameter(DEPTH_ID, -1)//
          .grab(config, x -> depth = x);
    }

    @Override
    public CFKppTree make() {
      return new CFKppTree(rnd, dist, depth, firstVar);
    }
  }
}
