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
package elki.clustering.hierarchical.birch;

import java.util.Random;

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.KMeansPlusPlus;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * K-Means++-like initialization for BIRCH k-means; this cannot be used to
 * initialize regular k-means, use {@link KMeansPlusPlus} instead.
 *
 * @author Andreas Lang
 * @since 0.8.0
 */
public class BIRCHKMeansPlusPlus {
  /**
   * Random generator
   */
  protected RandomFactory random;

  /**
   * Weights
   */
  protected double[] weights;

  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public BIRCHKMeansPlusPlus(RandomFactory rnd) {
    this.random = rnd;
  }

  /**
   * Perform k-means++ initialization.
   *
   * @param x Input vectors.
   * @param k K
   * @return Initial cluster centers
   */
  public double[][] run(final double[][] x, int k) {
    Random rnd = random.getSingleThreadedRandom();
    double[][] means = new double[k][];
    int first = rnd.nextInt(x.length);
    means[0] = x[first].clone();
    double weightsum = initialWeights(means[0], x);
    for(int m = 1; m < k; m++) {
      if(weightsum > Double.MAX_VALUE) {
        throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
      }
      double r = rnd.nextDouble() * weightsum;
      int i = 0;
      while(i < x.length) {
        if((r -= weights[i]) <= 0) {
          break;
        }
        i++;
      }
      if(i >= x.length) { // Rare case, but happens due to floating math
        weightsum -= r; // Decrease
        continue; // Retry
      }
      // Add new mean:
      means[m] = x[i].clone();
      if(m < k - 1) {
        // Update weights:
        weights[i] = 0.;
        weightsum = updateWeights(means[m], x);
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
  private double initialWeights(double[] first, double[][] x) {
    double weightsum = 0.;
    weights = new double[x.length];
    for(int i = 0; i < x.length; i++) {
      weightsum += weights[i] = distance(first, x[i]);
    }
    return weightsum;
  }

  /**
   * Calculates distance between two vectors.
   * 
   * @param ds First Vector
   * @param ds2 Second Vector
   * @return Vector
   */
  private double distance(double[] ds, double[] ds2) {
    double v = 0;
    for(int i = 0; i < ds.length; i++) {
      double d = ds[i] - ds2[i];
      v += d * d;
    }
    return v;
  }

  /**
   * Update the weight list.
   *
   * @param latest Latest center
   * @return Weight sum
   */
  private double updateWeights(double[] latest, double[][] x) {
    double weightsum = 0.;
    for(int i = 0; i < x.length; i++) {
      double weight = weights[i];
      if(weight <= 0.) {
        continue; // Duplicate, or already chosen.
      }
      double newweight = distance(latest, x[i]);
      weightsum += newweight < weight ? (weights[i] = newweight) : weight;
    }
    return weightsum;
  }

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(KMeans.SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public BIRCHKMeansPlusPlus make() {
      return new BIRCHKMeansPlusPlus(rnd);
    }
  }
}
