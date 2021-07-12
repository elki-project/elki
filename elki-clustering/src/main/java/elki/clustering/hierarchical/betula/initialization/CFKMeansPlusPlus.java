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

  boolean first_var;

  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public CFKMeansPlusPlus(RandomFactory rf, CFIDistance dist, boolean first_var) {
    super(rf);
    this.distance = dist;
    this.first_var = first_var;
  }

  @Override
  public double[][] chooseInitialMeans(CFTree<CFInterface> tree, CFInterface[] cfs, int k) {
    return run(tree, cfs, k);
  }

  /**
   * Perform k-means++ initialization.
   *
   * @param x Input vectors.
   * @param k K
   * @return Initial cluster centers
   */
  public double[][] run(CFTree<CFInterface> tree, final CFInterface[] cf, int k) {
    Random rnd = rf.getSingleThreadedRandom();
    double[][] means = new double[k][];
    int first;
    if(!first_var) {
      first = rnd.nextInt(cf.length);
    }
    else {
      first = sampleFirst(tree.getRoot(), cf, rnd);
    }
    int d = cf[first].getDimensionality();
    double[] mean = new double[d];
    for(int i = 0; i < d; i++) {
      mean[i] = cf[first].centroid(i);
    }
    means[0] = mean;
    double weightsum = initialWeights(cf[first], cf, distance);
    for(int m = 1; m < k; m++) {
      if(weightsum > Double.MAX_VALUE) {
        throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
      }
      double r = rnd.nextDouble() * weightsum;
      int i = 0;
      while(i < cf.length) {
        if((r -= weights[i]) <= 0) {
          break;
        }
        i++;
      }
      if(i >= cf.length) { // Rare case, but happens due to floating math
        weightsum -= r; // Decrease
        continue; // Retry
      }
      // Add new mean:
      mean = new double[d];
      for(int j = 0; j < cf[first].getDimensionality(); j++) {
        mean[j] = cf[i].centroid(j);
      }
      means[m] = mean;
      if(m < k) {
        // Update weights:
        weights[i] = 0.;
        weightsum = updateWeights(cf[i], cf, distance);
      }
    }
    return means;
  }

  /**
   * Initialize the weight list.
   * 
   * @param first Id of first mean.
   * @param x Input data.
   * @return Sum of weights
   */
  private double initialWeights(CFInterface first, CFInterface[] cf, CFIDistance distance) {
    double weightsum = 0.;
    weights = new double[cf.length];
    for(int i = 0; i < cf.length; i++) {
      weightsum += weights[i] = distance.squaredDistance(first, cf[i]);
    }
    return weightsum;
  }

  /**
   * Update the weight list.
   *
   * @param latest Latest center
   * @return Weight sum
   */
  private double updateWeights(CFInterface latest, CFInterface[] cf, CFIDistance distance) {
    double weightsum = 0.;
    for(int i = 0; i < cf.length; i++) {
      double weight = weights[i];
      if(weight <= 0.) {
        continue; // Duplicate, or already chosen.
      }
      double newweight = distance.squaredDistance(latest, cf[i]);
      weightsum += newweight < weight ? (weights[i] = newweight) : weight;
    }
    return weightsum;
  }

  int sampleFirst(CFInterface root, CFInterface[] cfs, Random rnd) {
    double weightsum = 0;
    double[] tmpWeight = new double[cfs.length];
    for(int i = 0; i < cfs.length; i++) {
      double weight;
      weight = distance.squaredDistance(root, cfs[i]);
      tmpWeight[i] = weight;
      weightsum += weight;
    }
    while(true) {
      double r = rnd.nextDouble() * weightsum;
      int i = 0;
      while(i < cfs.length) {
        if((r -= tmpWeight[i]) <= 0) {
          return i;
        }
        i++;
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
    public static final OptionID KMPlusPlus_ID = new OptionID("kmeans.distance", "Distance to use for kmeans++ criterion");

    public static final OptionID KMPFirst_ID = new OptionID("kmpp.first_var", "Chooose first dependent on Var");

    /**
     * Distance function to use for initial means
     */
    CFIDistance dist = null;

    boolean first_var = false;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
      new ObjectParameter<CFIDistance>(KMPlusPlus_ID, CFIDistance.class, EuclideanDist.class)//
          .grab(config, x -> dist = x);
      new Flag(KMPFirst_ID).grab(config, x -> first_var = x);
    }

    @Override
    public CFKMeansPlusPlus make() {
      return new CFKMeansPlusPlus(rnd, dist, first_var);
    }
  }
}
