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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import elki.index.tree.betula.CFNode;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Initialize K-means by following tree paths weighted by their variance
 * contribution. This is the strategy denoted "tree" in the reference.
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
@Alias("tree")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class CFKPlusPlusTree extends AbstractCFKMeansInitialization {
  /**
   * Distance function to use for initial means
   */
  CFInitWeight dist = null;

  /**
   * Choose the first center uniformly from the cluster features.
   */
  boolean firstUniform = false;

  /**
   * Maximum depth to choose at.
   */
  int maxdepth = -1;

  /**
   * Constructor.
   * 
   * @param dist distance function
   * @param firstUniform choose first center uniformly from the leaves
   * @param maxdepth maximum depth
   * @param rf random generator
   */
  public CFKPlusPlusTree(CFInitWeight dist, boolean firstUniform, int maxdepth, RandomFactory rf) {
    super(rf);
    this.dist = dist;
    this.maxdepth = maxdepth;
    this.firstUniform = firstUniform;
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends ClusterFeature> cfs, int k) {
    if(tree.numLeaves() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + tree.numLeaves() + " < k objects.");
    }
    maxdepth = maxdepth > 0 ? maxdepth : (int) Math.ceil(FastMath.log(k) / FastMath.log(tree.getCapacity())) + 1;
    List<ClusterFeature> ccs = new ArrayList<>(k);
    Random rnd = rf.getSingleThreadedRandom();
    // Choose first
    if(firstUniform) {
      // TODO: use weights for choosing the initial center?
      ccs.add(cfs.get(rnd.nextInt(cfs.size())).getCF());
    }
    else {
      List<ClusterFeature> rootcf = Arrays.asList(new ClusterFeature[] { tree.getRoot().getCF() });
      AsClusterFeature next = tree.getRoot();
      for(int depth = maxdepth; next instanceof CFNode && depth > 0; --depth) {
        next = chooseNextNode((CFNode<?>) next, rootcf, rnd);
      }
      ccs.add(next.getCF());
    }
    while(ccs.size() < k) {
      AsClusterFeature next = tree.getRoot();
      for(int depth = maxdepth; next instanceof CFNode && depth > 0; --depth) {
        next = chooseNextNode((CFNode<?>) next, ccs, rnd);
      }
      ccs.add(next.getCF());
    }
    // Convert CFs to means:
    double[][] means = new double[k][];
    for(int i = 0; i < k; i++) {
      means[i] = ccs.get(i).toArray();
    }
    return means;
  }

  /**
   * Choose a child of the current node.
   *
   * @param current Current node
   * @param ccs Currently chosen cluster centers
   * @param rnd Random generator
   * @return New cluster center
   */
  private AsClusterFeature chooseNextNode(CFNode<?> current, List<? extends ClusterFeature> ccs, Random rnd) {
    double weightsum = 0;
    double[] weights = new double[current.capacity()];
    int i = 0;
    while(true) {
      final AsClusterFeature child = current.getChild(i);
      if(child == null) {
        break;
      }
      double minweight = Double.POSITIVE_INFINITY;
      for(int j = 0; j < ccs.size(); j++) {
        final double weight = dist.squaredWeight(ccs.get(j).getCF(), child.getCF());
        minweight = weight < minweight ? weight : minweight;
      }
      weightsum += (weights[i++] = minweight);
    }

    while(true) {
      double r = rnd.nextDouble() * weightsum;
      for(int j = 0; j < i; j++) {
        if((r -= weights[j]) <= 0) {
          return current.getChild(j);
        }
      }
      weightsum -= r; // decrease and retry
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
    public static final OptionID KMPP_DISTANCE_ID = CFKPlusPlusLeaves.Par.KMPP_DISTANCE_ID;

    /**
     * Choose the first center based on variance contribution.
     */
    public static final OptionID FIRST_UNIFORM_ID = CFKPlusPlusLeaves.Par.FIRST_UNIFORM_ID;

    /**
     * Distance function to use for initial means
     */
    CFInitWeight dist = null;

    /**
     * Choose the first center uniformly from the cluster feature.
     */
    boolean firstUniform = false;

    /**
     * Maximum depth to choose from.
     */
    int depth = -1;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<CFInitWeight>(KMPP_DISTANCE_ID, CFInitWeight.class, VarianceWeight.class) //
          .grab(config, x -> dist = x);
      new Flag(FIRST_UNIFORM_ID).grab(config, x -> firstUniform = x);
      new IntParameter(DEPTH_ID, -1)//
          .grab(config, x -> depth = x);
    }

    @Override
    public CFKPlusPlusTree make() {
      return new CFKPlusPlusTree(dist, firstUniform, depth, rnd);
    }
  }
}
