package de.lmu.ifi.dbs.elki.data.uncertain;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * An abstract class used to define the basic format for Uncertain-Data-Objects.
 *
 * The SpatialComparable parameter asserts, that the Uncertain-Data-Objects from
 * classes derived from UOModel are fit for indexing via R-Trees.
 *
 * The method drawSample is planned to retrieve SamplePoints from a particular
 * Uncertain-Data-Object.
 *
 * To implement drawSample to retrieve such SamplePoints random, iterative or in
 * any other way is a matter of the particular author.
 *
 * The way one shapes his Uncertain-Data-Objects and there possible values isn't
 * of our concern, but drawSample shall return a {@link DoubleVector} for its
 * result to be easy to use with else ELKI algorithms.
 *
 * @author Alexander Koos
 */
public abstract class UncertainObject implements SpatialComparable, FeatureVector<Double> {
  /**
   * Compute the bounding box for some samples.
   *
   * @param samples Samples
   * @return Bounding box.
   */
  protected static HyperBoundingBox computeBounds(DoubleVector[] samples) {
    assert(samples.length > 0) : "Cannot compute bounding box of empty set.";
    // Compute bounds:
    final int dimensions = samples[0].getDimensionality();
    final double min[] = new double[dimensions];
    final double max[] = new double[dimensions];
    DoubleVector first = samples[0];
    for(int d = 0; d < dimensions; d++) {
      min[d] = max[d] = first.doubleValue(d);
    }
    for(int i = 1; i < samples.length; i++) {
      DoubleVector v = samples[i];
      for(int d = 0; d < dimensions; d++) {
        final double c = v.doubleValue(d);
        min[d] = c < min[d] ? c : min[d];
        max[d] = c > max[d] ? c : max[d];
      }
    }
    return new HyperBoundingBox(min, max);
  }

  protected SpatialComparable bounds;

  public static final double DEFAULT_MIN = Double.MIN_VALUE;

  public static final double DEFAULT_MAX = Double.MAX_VALUE;

  public static final int PROBABILITY_SCALE = 10000;

  public final static int DEFAULT_SAMPLE_SIZE = 10;

  public final static int DEFAULT_ENSEMBLE_DEPTH = 10;

  public final static int DEFAULT_TRY_LIMIT = 1000;

  public abstract DoubleVector drawSample(Random rand);

  public abstract DoubleVector getMean();

  @Override
  public double getMin(final int dimension) {
    return this.bounds.getMin(dimension);
  }

  @Override
  public double getMax(final int dimension) {
    return this.bounds.getMax(dimension);
  }

  @Override
  public Double getValue(int dimension) {
    // Center of bounding box.
    // Note: currently not used, part of the FeatureVector<Double> API.
    // But we are currently not implementing NumberVector!
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  @Override
  public int getDimensionality() {
    return this.bounds.getDimensionality();
  }

  /**
   * Vector factory.
   *
   * @author Erich Schubert
   *
   * @param <UO> Object type
   */
  public static abstract class Factory<UO extends UncertainObject> implements FeatureVector.Factory<UO, Double> {
    /**
     * Generate a bounding box for sampling.
     *
     * @param dim Dimensionality
     * @param minDev Minimum deviation
     * @param maxDev Maximum deviation
     * @param central Generate a symmetric distribution
     * @param drand Random generator
     * @return Offsets and ranges.
     */
    protected static double[] generateRandomRange(int dim, double minDev, double maxDev, boolean central, Random drand) {
      double[] offrange = new double[dim << 1];
      for(int j = 0; j < dim; j++) {
        double off = -1 * (drand.nextDouble() * (maxDev - minDev) + minDev);
        double range = central ? (-2 * off) : (-off + (drand.nextDouble() * (maxDev - minDev) + minDev));
        offrange[j++] = off;
        offrange[j++] = range;
      }
      return offrange;
    }

    boolean blur;

    @Override
    final public <A> UO newFeatureVector(A array, ArrayAdapter<? extends Double, A> adapter) {
      if(adapter instanceof NumberArrayAdapter) {
        return newFeatureVector(array, (NumberArrayAdapter<?, A>) adapter);
      }
      double[] f = new double[adapter.size(array)];
      for(int i = 0; i < f.length; i++) {
        f[i] = adapter.get(array, i);
      }
      return newFeatureVector(f, (NumberArrayAdapter<?, double[]>) ArrayLikeUtil.DOUBLEARRAYADAPTER);
    }

    abstract public <A> UO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter);

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    public abstract static class Parameterizer extends AbstractParameterizer {
      public static final OptionID MIN_MIN_ID = new OptionID("uo.uncertainty.min", "Minimum width of uncertain region.");

      public static final OptionID MAX_MIN_ID = new OptionID("uo.uncertainty.max", "Maximum width of uncertain region.");

      public static final OptionID MULT_MIN_ID = new OptionID("uo.quantity.min", "Minimum Points per uncertain object.");

      public static final OptionID MULT_MAX_ID = new OptionID("uo.quantity.max", "Maximum Points per uncertain object.");

      public static final OptionID SEED_ID = new OptionID("uo.seed", "Seed for uncertainification.");

      public static final OptionID MAXIMUM_PROBABILITY_ID = new OptionID("uo.maxprob", "Maximum total probability to draw a valid sample at all.");

    }
  }
}
