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
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract base implementation for {@link UncertainObject}s, providing shared
 * functionality such as bounding box access and random generation.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public abstract class AbstractUncertainObject implements UncertainObject {
  /**
   * Default retry limit for sampling, to guard against bad parameters.
   */
  public final static int DEFAULT_TRY_LIMIT = 1000;

  /**
   * Bounding box of the object.
   */
  protected SpatialComparable bounds;

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

  @Override
  public abstract DoubleVector drawSample(Random rand);

  @Override
  public int getDimensionality() {
    return bounds.getDimensionality();
  }

  @Override
  public double getMin(final int dimension) {
    return bounds.getMin(dimension);
  }

  @Override
  public double getMax(final int dimension) {
    return bounds.getMax(dimension);
  }

  @Override
  public Double getValue(int dimension) {
    // Center of bounding box.
    // Note: currently not used, part of the FeatureVector<Double> API.
    // But we are currently not implementing NumberVector!
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  @Override
  public abstract DoubleVector getMean();

  /**
   * Vector factory for uncertain objects.
   *
   * @author Erich Schubert
   *
   * @param <UO> Object type
   */
  public static abstract class Factory<UO extends UncertainObject> implements UncertainObject.Factory<UO> {
    /**
     * Only generate symmetric distributions.
     */
    boolean symmetric;

    /**
     * Constructor.
     *
     * @param symmetric Generate only symmetric distributions
     */
    public Factory(boolean symmetric) {
      super();
      this.symmetric = symmetric;
    }

    /**
     * Generate a bounding box for sampling.
     *
     * @param dim Dimensionality
     * @param minDev Minimum deviation
     * @param maxDev Maximum deviation
     * @param symmetric Generate a symmetric distribution
     * @param drand Random generator
     * @return Offsets and ranges.
     */
    protected static double[] generateRandomRange(int dim, double minDev, double maxDev, boolean symmetric, Random drand) {
      double[] offrange = new double[dim << 1];
      if(symmetric) {
        for(int j = 0; j < dim; j++) {
          double off = -1 * (drand.nextDouble() * (maxDev - minDev) + minDev);
          offrange[j++] = off;
          offrange[j++] = -2 * off;
        }
      }
      else {
        for(int j = 0; j < dim; j++) {
          double off = -1 * (drand.nextDouble() * (maxDev - minDev) + minDev);
          double range = (drand.nextDouble() * (maxDev - minDev) + minDev) - /* negative: */ off;
          offrange[j++] = off;
          offrange[j++] = range;
        }
      }
      return offrange;
    }

    @Override
    final public <A> UO newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      if(adapter instanceof NumberArrayAdapter) {
        return newFeatureVector(array, (NumberArrayAdapter<?, A>) adapter);
      }
      double[] f = new double[adapter.size(array)];
      for(int i = 0; i < f.length; i++) {
        f[i] = adapter.get(array, i).doubleValue();
      }
      return newFeatureVector(f, ArrayLikeUtil.DOUBLEARRAYADAPTER);
    }

    /**
     * Generate a new uncertain object. This interface is specialized to
     * numerical arrays.
     *
     * The generics allow the use with primitive {@code double[]} arrays:
     *
     * <pre>
     * UO obj = newFeatureVector(array, ArrayLikeUtil.DOUBLEARRAYADAPTER);
     * </pre>
     *
     * @param array Array
     * @param adapter Array type adapter
     * @param <A> Array type
     * @return Uncertain object
     */
    public abstract <A> UO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter);

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    public abstract static class Parameterizer extends AbstractParameterizer {
      /**
       * Default sample size for generating finite representations.
       */
      public final static int DEFAULT_SAMPLE_SIZE = 10;

      public static final OptionID MIN_MIN_ID = new OptionID("uo.uncertainty.min", "Minimum width of uncertain region.");

      public static final OptionID MAX_MIN_ID = new OptionID("uo.uncertainty.max", "Maximum width of uncertain region.");

      public static final OptionID MULT_MIN_ID = new OptionID("uo.quantity.min", "Minimum Points per uncertain object.");

      public static final OptionID MULT_MAX_ID = new OptionID("uo.quantity.max", "Maximum Points per uncertain object.");

      public static final OptionID SEED_ID = new OptionID("uo.seed", "Seed for uncertainification.");

      public static final OptionID SYMMETRIC_ID = new OptionID("uo.symmetric", "Only generate symmetric distributions based on the seed data.");

      @Override
      abstract protected Factory<?> makeInstance();
    }
  }
}
