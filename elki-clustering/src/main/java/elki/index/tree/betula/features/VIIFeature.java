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
 * Clustering Feature of stable BIRCH with a single variance per cluster
 * feature
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
public class VIIFeature implements ClusterFeature {
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
  double ssd;

  /**
   * Constructor.
   *
   * @param dimensionality Dimensionality
   */
  public VIIFeature(int dimensionality) {
    this.n = 0;
    this.mean = new double[dimensionality];
  }

  @Override
  public void addToStatistics(NumberVector nv) {
    final int d = nv.getDimensionality();
    assert (d == mean.length);
    if(n == 0) {
      for(int i = 0; i < d; i++) {
        mean[i] = nv.doubleValue(i);
      }
      ssd = 0;
      n++;
      return;
    }
    for(int i = 0; i < d; i++) {
      double v = nv.doubleValue(i);
      double delta = v - mean[i];
      double newmean = mean[i] += delta / (double) (n + 1.);
      ssd += delta * (v - newmean);
    }
    n++;
  }

  @Override
  public void addToStatistics(ClusterFeature other) {
    addToStatistics((VIIFeature) other);
  }

  // @Override
  public void addToStatistics(VIIFeature other) {
    if(this.n == 0) {
      for(int i = 0; i < mean.length; i++) {
        mean[i] = other.mean[i];
      }
      ssd = other.ssd;
      this.n = other.n;
      return;
    }
    assert (this.n > 0 && other.n > 0);
    double factor = other.n / (double) (n + other.n);
    double ssinc = 0;
    for(int i = 0; i < mean.length; i++) {
      double delta = other.mean[i] - mean[i];
      double newmean = mean[i] += delta * factor;
      ssinc += other.n * (delta * (other.mean[i] - newmean));
    }
    ssd += other.ssd + ssinc;
    this.n += other.n;
  }

  @Override
  public void resetStatistics() {
    n = 0;
    ssd = 0;
    Arrays.fill(mean, 0.);
  }

  @Override
  public double centroid(int i) {
    return mean[i];
  }

  @Override
  public double variance() {
    return ssd / n;
  }

  @Override
  public double sumdev() {
    return ssd;
  }

  @Override
  public double variance(int d) {
    double var = ssd / (mean.length * n);
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
  public double sumOfSquaredDev() {
    return ssd;
  }

  @Override
  public int getDimensionality() {
    return mean.length;
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
  @Alias("VII")
  public static class Factory implements ClusterFeature.Factory<VIIFeature> {
    /**
     * Static instance.
     */
    public static final Factory STATIC = new Factory();

    @Override
    public VIIFeature make(int dim) {
      return new VIIFeature(dim);
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
