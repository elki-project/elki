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

import java.util.List;
import java.util.Random;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.clustering.hierarchical.betula.CFTree;
import elki.clustering.kmeans.initialization.KMeansPlusPlus;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * K-Means++-like initialization for BIRCH k-means; this cannot be used to
 * initialize regular k-means, use {@link KMeansPlusPlus} instead.
 *
 * @author Andreas Lang
 */
public class CFKMeansPlusPlus extends AbstractCFKMeansInitialization {
  /**
   * Weights
   */
  protected double[] weights;

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
  public double[][] chooseInitialMeans(CFTree<?> tree, List<? extends CFInterface> cfs, int k) {
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
  public double[][] run(CFTree<?> tree, List<? extends CFInterface> cf, int k) {
    Random rnd = rf.getSingleThreadedRandom();
    double[][] means = new double[k][];
    CFInterface first = firstVar ? sampleFirst(tree.getRoot(), cf, rnd) : cf.get(rnd.nextInt(cf.size()));
    final int d = first.getDimensionality();
    double[] mean = means[0] = new double[d];
    for(int i = 0; i < d; i++) {
      mean[i] = first.centroid(i);
    }
    double weightsum = initialWeights(first, cf);
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
      CFInterface cfi = cf.get(i);
      // Add new mean:
      mean = means[m] = new double[d];
      for(int j = 0; j < mean.length; j++) {
        mean[j] = cfi.centroid(j);
      }
      if(m < k) {
        // Update weights:
        weights[i] = 0.;
        weightsum = updateWeights(cfi, cf);
      }
    }
    return means;
  }

  /**
   * Initialize the weight list.
   * 
   * @param first Id of first mean.
   * @param cf Cluster features
   * @return Sum of weights
   */
  private double initialWeights(CFInterface first, List<? extends CFInterface> cf) {
    final int e = cf.size();
    double weightsum = 0.;
    weights = new double[e];
    for(int i = 0; i < e; i++) {
      weightsum += weights[i] = distance.squaredDistance(first, cf.get(i));
    }
    return weightsum;
  }

  /**
   * Update the weight list.
   *
   * @param latest Latest center
   * @param cf Cluster features
   * @return Weight sum
   */
  private double updateWeights(CFInterface latest, List<? extends CFInterface> cf) {
    double weightsum = 0.;
    for(int i = 0, e = cf.size(); i < e; i++) {
      double weight = weights[i];
      if(weight <= 0.) {
        continue; // Duplicate, or already chosen.
      }
      double newweight = distance.squaredDistance(latest, cf.get(i));
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
  private CFInterface sampleFirst(CFInterface root, List<? extends CFInterface> cfs, Random rnd) {
    final int e = cfs.size();
    double weightsum = 0;
    double[] tmpWeight = new double[e];
    for(int i = 0; i < e; i++) {
      double weight = distance.squaredDistance(root, cfs.get(i));
      tmpWeight[i] = weight;
      weightsum += weight;
    }
    while(true) {
      double r = rnd.nextDouble() * weightsum;
      for(int i = 0; i < e; i++) {
        if((r -= tmpWeight[i]) <= 0) {
          return cfs.get(i);
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
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
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
