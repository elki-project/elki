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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

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
public abstract class UOModel implements SpatialComparable {
  protected Random rand;

  protected SpatialComparable bounds;

  protected int dimensions;

  public static final double DEFAULT_MIN = Double.MIN_VALUE;

  public static final double DEFAULT_MAX = Double.MAX_VALUE;

  public static final int PROBABILITY_SCALE = 10000;

  public final static long DEFAULT_MIN_MAX_DEVIATION = 5l;

  public final static long DEFAULT_MIN_MAX_DEVIATION_GAUSSIAN = 3l;

  public final static long DEFAULT_SAMPLE_SIZE = 10l;

  public final static long DEFAULT_STDDEV = 1l;

  public final static long DEFAULT_MULTIPLICITY = 1l;

  public final static int DEFAULT_PROBABILITY_SEED = 5;

  public final static double DEFAULT_MAX_TOTAL_PROBABILITY = 1.0;

  public final static int DEFAULT_ENSEMBLE_DEPTH = 10;

  public final static int DEFAULT_TRY_LIMIT = 1000;

  public abstract DoubleVector drawSample();

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
   * This method's purpose is to be implemented by particular uncertainity
   * models in a way, they can be used as parameters to create an uncertain
   * dataset out of a given certain dataset (a groundtruth by any means).
   *
   * @param vec
   * @param blur
   *
   * @return
   */
  public abstract UncertainObject<UOModel> uncertainify(NumberVector vec, boolean blur);
}
