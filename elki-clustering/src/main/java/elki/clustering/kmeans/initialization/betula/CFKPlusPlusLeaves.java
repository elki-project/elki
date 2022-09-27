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

import java.util.List;
import java.util.Random;

import elki.clustering.kmeans.initialization.KMeansPlusPlus;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

/**
 * K-Means++-like initialization for BETULA k-means, treating the leaf
 * clustering features as a flat list, and called "leaves" in the publication.
 * To initialize regular k-means, use {@link KMeansPlusPlus} instead.
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
@Alias("leaves")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class CFKPlusPlusLeaves extends AbstractCFKMeansInitialization {
  /**
   * Distance function
   */
  protected CFInitWeight distance;

  /**
   * Choose the first center uniformly from the leaves.
   */
  protected boolean firstUniform;

  /**
   * Constructor.
   * 
   * @param dist distance function
   * @param firstUniform choose the first center uniformly from leaves
   * @param rf random generator
   */
  public CFKPlusPlusLeaves(CFInitWeight dist, boolean firstUniform, RandomFactory rf) {
    super(rf);
    this.distance = dist;
    this.firstUniform = firstUniform;
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends ClusterFeature> cfs, int k) {
    return run(tree, cfs, k);
  }

  /**
   * Perform k-means++ initialization.
   *
   * @param tree CFTree
   * @param cfs Cluster features
   * @param k K
   * @return Initial cluster centers
   */
  public double[][] run(CFTree<?> tree, List<? extends ClusterFeature> cfs, int k) {
    Random rnd = rf.getSingleThreadedRandom();
    double[][] means = new double[k][];
    ClusterFeature first = firstUniform ? cfs.get(rnd.nextInt(cfs.size())).getCF() : sampleFirst(tree.getRoot().getCF(), cfs, rnd);
    means[0] = first.toArray();
    double[] weights = new double[cfs.size()];
    double weightsum = initialWeights(first, cfs, weights);
    for(int m = 1; m < k; m++) {
      if(weightsum > Double.MAX_VALUE) {
        throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
      }
      double r = rnd.nextDouble() * weightsum;
      int i = 0;
      while(i < cfs.size()) {
        if((r -= weights[i]) <= 0) {
          break;
        }
        i++;
      }
      if(i >= cfs.size()) { // Rare case, but happens due to floating math
        weightsum -= r; // Decrease
        continue; // Retry
      }
      ClusterFeature cfi = cfs.get(i).getCF();
      means[m] = cfi.toArray();
      if(m < k - 1) {
        weights[i] = 0.; // disable
        weightsum = updateWeights(cfi, cfs, weights);
      }
    }
    return means;
  }

  /**
   * Sample the first cluster center.
   *
   * @param root Root node of the tree
   * @param cfs Cluster features to sample from
   * @param rnd Random generator
   * @return Selected cluster feature
   */
  private ClusterFeature sampleFirst(ClusterFeature root, List<? extends AsClusterFeature> cfs, Random rnd) {
    double weightsum = 0;
    double[] tmpWeight = new double[cfs.size()];
    {
      int i = 0;
      for(AsClusterFeature cf : cfs) {
        weightsum += tmpWeight[i++] = distance.squaredWeight(root, cf.getCF());
      }
    }
    while(true) {
      double r = rnd.nextDouble() * weightsum;
      for(int i = 0; i < tmpWeight.length; i++) {
        if((r -= tmpWeight[i]) <= 0) {
          return cfs.get(i).getCF();
        }
      }
      weightsum -= r; // Shouldn't happen. Decrease and retry
    }
  }

  /**
   * Initialize the weight list.
   * 
   * @param first Id of first mean.
   * @param cfs Cluster features
   * @param weights Weights output
   * @return Sum of weights
   */
  private double initialWeights(ClusterFeature first, List<? extends AsClusterFeature> cfs, double[] weights) {
    double weightsum = 0.;
    int i = 0;
    for(AsClusterFeature cf : cfs) {
      weightsum += weights[i++] = distance.squaredWeight(first, cf.getCF());
    }
    return weightsum;
  }

  /**
   * Update the weight list.
   *
   * @param latest Latest center
   * @param cfs Cluster features
   * @param weights Weights
   * @return Weight sum
   */
  private double updateWeights(ClusterFeature latest, List<? extends AsClusterFeature> cfs, double[] weights) {
    double weightsum = 0.;
    int i = -1; // incremented below
    for(AsClusterFeature cf : cfs) {
      double weight = weights[++i];
      if(weight <= 0.) {
        continue; // Duplicate, or already chosen.
      }
      double newweight = distance.squaredWeight(latest, cf.getCF());
      weightsum += newweight < weight ? (weights[i] = newweight) : weight;
    }
    return weightsum;
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
    public static final OptionID KMPP_DISTANCE_ID = new OptionID("kmeans.distance", "Distance to use for kmeans++ criterion");

    /**
     * Choose the first center based on variance contribution.
     */
    public static final OptionID FIRST_UNIFORM_ID = new OptionID("kmpp.first-uniform", "Choose the first center uniformly from the cluster features.");

    /**
     * Distance function to use for initial means
     */
    CFInitWeight dist = null;

    /**
     * Choose the first center based on variance contribution.
     */
    boolean firstUniform = false;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<CFInitWeight>(KMPP_DISTANCE_ID, CFInitWeight.class, SquaredEuclideanWeight.class)//
          .grab(config, x -> dist = x);
      new Flag(FIRST_UNIFORM_ID).grab(config, x -> firstUniform = x);
    }

    @Override
    public CFKPlusPlusLeaves make() {
      return new CFKPlusPlusLeaves(dist, firstUniform, rnd);
    }
  }
}
