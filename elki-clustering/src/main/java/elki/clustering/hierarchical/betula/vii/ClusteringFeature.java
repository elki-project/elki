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
package elki.clustering.hierarchical.betula.vii;

import java.util.Arrays;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.data.NumberVector;

/**
 * Clustering Feature of stable BIRCH with a single variance per cluster
 * feature
 * 
 * @author Andreas Lang
 */
public class ClusteringFeature implements CFInterface {
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
  public ClusteringFeature(int dimensionality) {
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
  public void addToStatistics(CFInterface other) {
    addToStatistics((ClusteringFeature) other);
  }

  // @Override
  public void addToStatistics(ClusteringFeature other) {
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
  public double squaredCenterDistance(NumberVector v) {
    double sum = 0;
    for(int d = 0, dim = mean.length; d < dim; d++) {
      final double delta = mean[d] - v.doubleValue(d);
      sum += delta * delta;
    }
    return sum;
  }

  @Override
  public double squaredCenterDistance(CFInterface other) {
    double[] omean = ((ClusteringFeature) other).mean;
    double sum = 0;
    for(int d = 0, dim = mean.length; d < dim; d++) {
      final double delta = mean[d] - omean[d];
      sum += delta * delta;
    }
    return sum;
  }

  @Override
  public double absoluteCenterDistance(NumberVector v) {
    double sum = 0;
    for(int d = 0, dim = mean.length; d < dim; d++) {
      final double delta = mean[d] - v.doubleValue(d);
      sum += Math.abs(delta);
    }
    return sum;
  }

  @Override
  public double absoluteCenterDistance(CFInterface other) {
    double[] omean = ((ClusteringFeature) other).mean;
    double sum = 0;
    for(int d = 0, dim = mean.length; d < dim; d++) {
      final double delta = mean[d] - omean[d];
      sum += Math.abs(delta);
    }
    return sum;
  }
}
