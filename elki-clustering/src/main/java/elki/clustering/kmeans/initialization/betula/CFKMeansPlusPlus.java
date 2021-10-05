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

import java.util.List;
import java.util.Random;

import elki.clustering.kmeans.initialization.KMeansPlusPlus;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

/**
 * K-Means++-like initialization for BETULA k-means, treating the clustering
 * features as a flat list; this cannot be used to initialize regular k-means,
 * use {@link KMeansPlusPlus} instead.
 *
 * @author Andreas Lang
 */
public class CFKMeansPlusPlus extends AbstractCFKMeansInitialization {
  /**
   * Distance function
   */
  protected CFIDistance distance;

  /**
   * Choose the first center based on variance contribution.
   */
  protected boolean firstVar;

  /**
   * Constructor.
   *
   * @param rf Random generator
   * @param dist Distance function
   * @param firstVar Choose first based on variance
   */
  public CFKMeansPlusPlus(RandomFactory rf, CFIDistance dist, boolean firstVar) {
    super(rf);
    this.distance = dist;
    this.firstVar = firstVar;
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends AsClusterFeature> cfs, int k) {
    return run(tree, cfs, k);
  }

  /**
   * Perform k-means++ initialization.
   *
   * @param tree CFTree
   * @param cf cluster features to choose from (should be an array list for
   *        performance reasons)
   * @param k K
   * @return Initial cluster centers
   */
  public double[][] run(CFTree<?> tree, List<? extends AsClusterFeature> cf, int k) {
    Random rnd = rf.getSingleThreadedRandom();
    double[][] means = new double[k][];
    ClusterFeature first = firstVar ? sampleFirst(tree.getRoot().getCF(), cf, rnd) : cf.get(rnd.nextInt(cf.size())).getCF();
    means[0] = first.toArray();
    double[] weights = new double[cf.size()];
    double weightsum = initialWeights(first, cf, weights);
    for(int m = 1; m < k; m++) {
      if(weightsum > Double.MAX_VALUE) {
        throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
      }
      double r = rnd.nextDouble() * weightsum;
      int i = 0;
      while(i < cf.size()) {
        if((r -= weights[i]) <= 0) {
          break;
        }
        i++;
      }
      if(i >= cf.size()) { // Rare case, but happens due to floating math
        weightsum -= r; // Decrease
        continue; // Retry
      }
      ClusterFeature cfi = cf.get(i).getCF();
      means[m] = cfi.toArray();
      if(m < k) {
        weights[i] = 0.; // disable
        weightsum = updateWeights(cfi, cf, weights);
      }
    }
    return means;
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
      weightsum += weights[i++] = distance.squaredDistance(first, cf.getCF());
    }
    return weightsum;
  }

  /**
   * Update the weight list.
   *
   * @param latest Latest center
   * @param cf Cluster features
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
      double newweight = distance.squaredDistance(latest, cf.getCF());
      weightsum += newweight < weight ? (weights[i] = newweight) : weight;
    }
    return weightsum;
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
        weightsum += tmpWeight[i++] = distance.squaredDistance(root, cf.getCF());
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
    public static final OptionID FIRST_VARIANCE_ID = new OptionID("kmpp.first_var", "Chooose first dependent on variance.");

    /**
     * Distance function to use for initial means
     */
    CFIDistance dist = null;

    /**
     * Choose the first center based on variance contribution.
     */
    boolean firstVar = false;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<CFIDistance>(KMPP_DISTANCE_ID, CFIDistance.class, EuclideanDistance.class)//
          .grab(config, x -> dist = x);
      new Flag(FIRST_VARIANCE_ID).grab(config, x -> firstVar = x);
    }

    @Override
    public CFKMeansPlusPlus make() {
      return new CFKMeansPlusPlus(rnd, dist, firstVar);
    }
  }
}
