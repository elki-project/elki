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
package elki.clustering.hierarchical.betula.vvv;

import java.util.Arrays;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.data.NumberVector;

/**
 * Clustering Feature of stable BIRCH with covariance instead of variance
 * 
 * @author Andreas Lang
 * 
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
  double[][] ssd;

  /**
   * Constructor.
   *
   * @param dimensionality Dimensionality
   */
  public ClusteringFeature(int dimensionality) {
    this.n = 0;
    this.ssd = new double[dimensionality][dimensionality];
    this.mean = new double[dimensionality];
  }

  @Override
  public void addToStatistics(NumberVector nv) {
    final int d = nv.getDimensionality();
    assert (d == ssd.length);
    if(n == 0) {
      for(int i = 0; i < d; i++) {
        for(int j = 0; j < d; j++) {
          ssd[i][j] = 0.;
        }
        mean[i] = nv.doubleValue(i);
      }
      n++;
      return;
    }
    double[] nmea = new double[d];
    double f = 1. / (n + 1.);
    for(int i = 0; i < d; i++) {
      double vi = nv.doubleValue(i);
      double delta = vi - mean[i];
      nmea[i] = mean[i] + delta * f;
      for(int j = 0; j <= i; j++) {
        ssd[i][j] += delta * (nv.doubleValue(j) - nmea[j]);
      }
    }
    for(int i = 0; i < d; i++) {
      for(int j = 0; j < i; j++) {
        ssd[j][i] = ssd[i][j];
      }
    }
    n++;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void addToStatistics(CFInterface other) {
    addToStatistics((ClusteringFeature) other);

  }

  // @Override
  public void addToStatistics(ClusteringFeature other) {
    if(this.n == 0) {
      for(int i = 0; i < ssd.length; i++) {
        for(int j = 0; j < ssd.length; j++) {
          ssd[i][j] = other.ssd[i][j];
        }
        mean[i] = other.mean[i];
      }
      this.n = other.n;
      return;
    }
    assert (this.n > 0 && other.n > 0);
    double[] nmea = new double[mean.length];
    double factor = other.n / (double) (n + other.n);
    for(int i = 0; i < ssd.length; i++) {
      double delta = other.mean[i] - mean[i];
      nmea[i] = mean[i] + delta * factor;
      for(int j = 0; j <= i; j++) {
        ssd[i][j] += other.ssd[i][j] + other.n * (delta * (other.mean[j] - nmea[j]));
      }
    }
    for(int i = 0; i < ssd.length; i++) {
      for(int j = 0; j < i; j++) {
        ssd[j][i] = ssd[i][j];
      }
    }
    this.n += other.n;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void resetStatistics() {
    n = 0;
    for(int i = 0; i < ssd.length; i++) {
      Arrays.fill(ssd[i], 0.);
    }
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
      var += ssd[i][i];
    }
    return var / n;
  }

  @Override
  public double sumdev() {
    double var = 0.;
    for(int i = 0; i < ssd.length; i++) {
      var += ssd[i][i];
    }
    return var;
  }

  @Override
  public double variance(int d) {
    double var = ssd[d][d] / n;
    return var >= 0. ? var : 0.;
  }

  @Override
  public double[][] covariance() {
    double[][] cov = new double[mean.length][mean.length];
    final double f = 1. / n;
    for(int i = 0; i < mean.length; i++) {
      for(int j = 0; j < mean.length; j++) {
        cov[i][j] = ssd[i][j] * f;
      }
    }
    return cov;
  }

  @Override
  public int getDimensionality() {
    return mean.length;
  }

  @Override
  public int getWeight() {
    return n;
  }

}
