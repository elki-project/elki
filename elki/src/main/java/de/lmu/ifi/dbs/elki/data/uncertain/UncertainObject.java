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
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

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
  protected SpatialComparable bounds;

  protected int dimensions;

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
  public int getDimensionality() {
    return this.dimensions;
  }

  /**
   * Vector factory.
   *
   * @author Erich Schubert
   *
   * @param <UO> Object type
   */
  public static abstract class Factory<UO extends UncertainObject> implements FeatureVector.Factory<UO, Double> {
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
  }
}
