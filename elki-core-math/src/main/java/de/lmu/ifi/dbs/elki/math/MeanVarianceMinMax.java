/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.math;

/**
 * Class collecting mean, variance, minimum and maximum statistics.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class MeanVarianceMinMax extends MeanVariance {
  /**
   * Minimum value
   */
  double min;

  /**
   * Maximum value
   */
  double max;

  /**
   * Constructor.
   */
  public MeanVarianceMinMax() {
    super();
    min = Double.POSITIVE_INFINITY;
    max = Double.NEGATIVE_INFINITY;
  }

  /**
   * Constructor cloning existing statistics.
   * 
   * @param other Existing statistics
   */
  public MeanVarianceMinMax(MeanVarianceMinMax other) {
    super(other);
    this.min = other.min;
    this.max = other.max;
  }

  @Override
  public void put(double val) {
    super.put(val);
    min = val < min ? val : min;
    max = val > max ? val : max;
  }

  @Override
  public void put(double val, double weight) {
    super.put(val, weight);
    if(weight <= 0) {
      return;
    }
    min = val < min ? val : min;
    max = val > max ? val : max;
  }

  @Override
  public void put(Mean other) {
    if(!(other instanceof MeanVarianceMinMax)) {
      throw new IllegalArgumentException("Cannot aggregate into a minmax statistic: " + other.getClass());
    }
    super.put(other);
    MeanVarianceMinMax om = (MeanVarianceMinMax) other;
    min = om.min < min ? om.min : min;
    max = om.max > max ? om.max : max;
  }

  /**
   * Add values with weight 1.0
   * 
   * @param vals Values
   * @return this
   */
  @Override
  public MeanVarianceMinMax put(double[] vals) {
    final int l = vals.length;
    if(l < 2) {
      if(l == 1) {
        put(vals[0]);
      }
      return this;
    }
    // First pass:
    double s1 = 0.;
    for(int i = 0; i < l; i++) {
      final double v = vals[i];
      s1 += v;
      min = v < min ? v : min;
      max = v > max ? v : max;
    }
    final double om1 = s1 / l;
    // Second pass:
    double om2 = 0., err = 0.;
    for(int i = 0; i < l; i++) {
      final double v = vals[i] - om1;
      om2 += v * v;
      err += v;
    }
    s1 += err;
    om2 += err / l;
    if(n <= 0) {
      n = l;
      sum = s1;
      m2 = om2;
      return this;
    }
    final double tmp = n * s1 - sum * l;
    final double oldn = n; // tmp copy
    n += l;
    sum += s1 + err;
    m2 += om2 + tmp * tmp / (l * n * oldn);
    return this;
  }

  @Override
  public MeanVariance put(double[] vals, double[] weights) {
    assert (vals.length == weights.length);
    for(int i = 0, end = vals.length; i < end; i++) {
      // TODO: use a two-pass update as in the other put
      put(vals[i], weights[i]);
    }
    return this;
  }

  /**
   * Get the current minimum.
   * 
   * @return current minimum.
   */
  public double getMin() {
    return this.min;
  }

  /**
   * Get the current maximum.
   * 
   * @return current maximum.
   */
  public double getMax() {
    return this.max;
  }

  /**
   * Get the current minimum and maximum.
   * 
   * @return current minimum and maximum
   */
  public DoubleMinMax getDoubleMinMax() {
    return new DoubleMinMax(this.min, this.max);
  }

  /**
   * Return the difference between minimum and maximum.
   * 
   * @return Difference of current Minimum and Maximum.
   */
  public double getDiff() {
    return this.getMax() - this.getMin();
  }

  /**
   * Create and initialize a new array of MeanVarianceMinMax
   * 
   * @param dimensionality Dimensionality
   * @return New and initialized Array
   */
  public static MeanVarianceMinMax[] newArray(int dimensionality) {
    MeanVarianceMinMax[] arr = new MeanVarianceMinMax[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      arr[i] = new MeanVarianceMinMax();
    }
    return arr;
  }

  @Override
  public String toString() {
    return "MeanVarianceMinMax(mean=" + getMean() + ",var=" + getNaiveVariance() + ",min=" + getMin() + ",max=" + getMax() + ",weight=" + n + ")";
  }

  @Override
  public void reset() {
    super.reset();
    min = Double.POSITIVE_INFINITY;
    max = Double.NEGATIVE_INFINITY;
  }
}
