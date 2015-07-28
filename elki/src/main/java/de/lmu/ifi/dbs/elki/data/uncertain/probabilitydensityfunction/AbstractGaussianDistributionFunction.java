package de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Abstract class for gaussian models for use in
 * uncertain objects.
 *
 * Primary target was to reduce code duplicity regarding
 * multivariate gaussian distributions and dimensional
 * independent gaussian distributions.
 *
 * Those two cases are separated to not use to fat
 * (containing {@link List} of {@link Matrix} for
 * simple univariate gaussian distributions per
 * default.
 *
 * @author Alexander Koos
 */
public abstract class AbstractGaussianDistributionFunction<V> extends ProbabilityDensityFunction {

  /**
   * Field to hold the value the randomly created variance shall have
   * in minimum.
   */
  protected double minDev;

  /**
   * Field to hold the value the randomly created variance shall have
   * in maximum.
   */
  protected double maxDev;

  /**
   * Field to hold the value the randomly created maximum negative
   * deviation from the groundtruth shall have in minimum.
   */
  protected double minMin;

  /**
   * Field to hold the value the randomly created maximum negative
   * deviation from the groundtruth shall have in maximum.
   */
  protected double maxMin;

  /**
   * Field to hold the value the randomly created maximum positive
   * deviation from the groundtruth shall have in minimum.
   */
  protected double minMax;

  /**
   * Field to hold the value the randomly created maximum positive
   * deviation from the groundtruth shall have in maximum.
   */
  protected double maxMax;

  /**
   * Field to hold the value the randomly created multiplicity shall
   * have in minimum.
   */
  protected long multMin;

  /**
   * Field to hold the value the randomly created multiplicity shall
   * have in maximum.
   */
  protected long multMax;

  /**
   * Field to hold a {@link Random} used for uncertainification.
   */
  protected Random urand;


  protected List<DoubleVector> means;
  protected List<V> variances;
  protected int[] weights;
  protected int weightMax = UOModel.PROBABILITY_SCALE; // reset by constructor
  // in case no bounded sample has been found
  protected static DoubleVector noSample = new DoubleVector(new double[] {Double.NEGATIVE_INFINITY});

  public void setMeans(final List<DoubleVector> means) {
    this.means = means;
  }

  public List<DoubleVector> getMeans() {
    return this.means;
  }

  public DoubleVector getMean(final int position) {
    return this.means.get(position);
  }

  public void setMean(final int position, final DoubleVector mean) {
    this.means.set(position, mean);
  }

  public void setWeights(final int[] weights) {
    this.weights = weights;
  }

  public int[] getWeights() {
    return this.weights;
  }

  public int getWeight(final int position) {
    return this.weights[position];
  }

  public void setVariances(final List<V> variances) {
    this.variances = variances;
  }

  public List<V> getVariances() {
    return this.variances;
  }

  public V getVariances(final int position) {
    return this.variances.get(position);
  }

  protected abstract List<DoubleVector> getDeviationVector();

  protected SpatialComparable getDefaultBounds(int dimensions, List<DoubleVector> means, List<DoubleVector> deviationVector) {
    double[] min = new double[dimensions];
    double[] max = new double[dimensions];
    double localExtreme;
    for(int i = 0; i < dimensions; i++) {
      min[i] = Double.MAX_VALUE;
      max[i] = Double.MIN_VALUE;
    }
    for(int i = 0; i < means.size(); i++) {
      for(int j = 0; j < dimensions; j++) {
        localExtreme = means.get(i).doubleValue(j) - deviationVector.get(i).doubleValue((deviationVector.get(i).getDimensionality() > 1 ? j : 0)) * 3;
        min[j] = localExtreme < min[j] ? localExtreme : min[j];
        localExtreme = means.get(i).doubleValue(j) + deviationVector.get(i).doubleValue((deviationVector.get(i).getDimensionality() > 1 ? j : 0)) * 3;
        max[j] = localExtreme > max[j] ? localExtreme : max[j];
      }
    }
    return new HyperBoundingBox(min, max);
  }

  @Override
  public SpatialComparable getDefaultBounds(final int dimensions) {
    return getDefaultBounds(dimensions, getMeans(), getDeviationVector());
  }
}
