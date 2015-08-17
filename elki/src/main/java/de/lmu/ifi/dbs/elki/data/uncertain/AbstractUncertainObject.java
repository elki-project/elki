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
     * Constructor.
     */
    public Factory() {
      super();
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
        for(int i = 0, j = 0; i < dim; ++i) {
          double off = -1 * (drand.nextDouble() * (maxDev - minDev) + minDev);
          offrange[j++] = off;
          offrange[j++] = -2 * off;
        }
      }
      else {
        for(int i = 0, j = 0; i < dim; ++i) {
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
      public static final OptionID DEV_MIN_ID = new OptionID("uo.uncertainty.min", "Minimum width of uncertain region.");

      public static final OptionID DEV_MAX_ID = new OptionID("uo.uncertainty.max", "Maximum width of uncertain region.");

      public static final OptionID SEED_ID = new OptionID("uo.seed", "Seed for uncertainification.");

      public static final OptionID SYMMETRIC_ID = new OptionID("uo.symmetric", "Only generate symmetric distributions based on the seed data.");

      @Override
      abstract protected Factory<?> makeInstance();
    }
  }
}
