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
package elki.index.tree.betula.features;

import java.util.Arrays;

import elki.data.NumberVector;
import elki.utilities.Alias;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Clustering Feature of stable BIRCH with variance per dimension
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
public class VVIFeature implements ClusterFeature {
  /**
   * Number of objects
   */
  int n;

  /**
   * mean
   */
  double[] mean;

  /**
   * Sum of Squared Deviations.
   */
  double[] ssd;

  /**
   * Constructor.
   *
   * @param dimensionality Dimensionality
   */
  public VVIFeature(int dimensionality) {
    this.n = 0;
    this.ssd = new double[dimensionality];
    this.mean = new double[dimensionality];
  }

  @Override
  public void addToStatistics(NumberVector nv) {
    final int d = nv.getDimensionality();
    assert (d == ssd.length);
    if(n == 0) {
      for(int i = 0; i < d; i++) {
        ssd[i] = 0.;
        mean[i] = nv.doubleValue(i);
      }
      n++;
      return;
    }
    for(int i = 0; i < d; i++) {
      double v = nv.doubleValue(i);
      double delta = v - mean[i];
      double newmean = mean[i] += delta / (double) (n + 1.);
      ssd[i] += delta * (v - newmean);
    }
    n++;
  }

  @Override
  public void addToStatistics(ClusterFeature other) {
    addToStatistics((VVIFeature) other);
  }

  // @Override
  public void addToStatistics(VVIFeature other) {
    if(this.n == 0) {
      for(int i = 0; i < ssd.length; i++) {
        ssd[i] = other.ssd[i];
        mean[i] = other.mean[i];
      }
      this.n = other.n;
      return;
    }
    assert (this.n > 0 && other.n > 0);
    double factor = other.n / (double) (n + other.n);
    for(int i = 0; i < ssd.length; i++) {
      double delta = other.mean[i] - mean[i];
      double newmean = mean[i] += delta * factor;
      ssd[i] += other.ssd[i] + other.n * (delta * (other.mean[i] - newmean));
    }
    this.n += other.n;
  }

  @Override
  public void resetStatistics() {
    n = 0;
    Arrays.fill(ssd, 0.);
    Arrays.fill(mean, 0.);
  }

  @Override
  public double centroid(int i) {
    return mean[i];
  }

  @Override
  public double variance() {
    double var = 0.;
    for(int i = 0; i < ssd.length; i++) {
      var += ssd[i];
    }
    return var / n;
  }

  @Override
  public double sumdev() {
    double var = 0.;
    for(int i = 0; i < ssd.length; i++) {
      var += ssd[i];
    }
    return var;
  }

  @Override
  public double variance(int d) {
    double var = ssd[d] / n;
    return var >= 0. ? var : 0.;
  }

  @Override
  public double[][] covariance() {
    throw new IllegalStateException("This CF Model doesn't support this method.");
  }

  /**
   * Sum of Squared Deviations.
   *
   * @return Sum of Squared Deviations.
   */
  public double sumOfSquaredDev(int i) {
    return ssd[i];
  }

  @Override
  public int getDimensionality() {
    return ssd.length;
  }

  @Override
  public int getWeight() {
    return n;
  }

  @Override
  public double[] toArray() {
    return mean.clone();
  }

  /**
   * Factory for making cluster features.
   * 
   * @author Erich Schubert
   */
  @Alias("VVI")
  public static class Factory implements ClusterFeature.Factory<VVIFeature> {
    /**
     * Static instance.
     */
    public static final Factory STATIC = new Factory();

    @Override
    public VVIFeature make(int dim) {
      return new VVIFeature(dim);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par implements Parameterizer {
      @Override
      public Factory make() {
        return STATIC;
      }
    }
  }
}
