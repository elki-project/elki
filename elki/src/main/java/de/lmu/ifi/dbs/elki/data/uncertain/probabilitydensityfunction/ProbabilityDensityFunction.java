package de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

/**
 *
 * Abstract class specifying the minimum constraints to the
 * layout for probability density functions used in uncertain
 * objects.
 *
 * @author Alexander Koos
 *
 */
public abstract class ProbabilityDensityFunction implements TextWriteable {

  /**
   * Used in methods for setting bounds to initialize the
   * array used in search for extreme points.
   *
   * E.g. {@link UniformDistributionFunction#getDefaultBounds(int)}
   */
  final public static double DEFAULT_MIN = Double.MIN_VALUE;

  /**
   * Take note of {@link ProbabilityDensityFunction#DEFAULT_MIN}
   */
  final public static double DEFAULT_MAX = Double.MAX_VALUE;

  /**
   * Draw a sample, constraint by bounds and using rand for
   * random numbers, to use for algorithms on uncertain data.
   */
  public abstract DoubleVector drawValue(SpatialComparable bounds, final Random rand);

  /**
   *
   * In case no bounds are explicitly given create some via
   * a default metric with dimensionality of dimensions.
   *
   * @param dimensions
   * @return
   */
  public abstract SpatialComparable getDefaultBounds(final int dimensions);

  /**
   * Construct an {@link UncertainObject} from a given vector.
   * @param blur TODO
   * @param uncertainify TODO
   * @param dims TODO
   */
  public abstract UncertainObject<UOModel> uncertainify(NumberVector vec, boolean blur, boolean uncertainify, int dims);

  /**
   *
   * For visualization create a vector ankering the uncertain
   * object to a discrete point its space.
   *
   * This method is to be used if you want to anker the
   * uncertain object to a point representing it well instead
   * of the point that has been uncertainified to create the
   * uncertain object.
   *
   * Default implementations of {@link ProbabilityDensityFunction#uncertainify}
   * store their argument vec as anker in the created {@link UncertainObject}.
   *
   * @param bounds
   * @return
   */
  public DoubleVector getAnker(final SpatialComparable bounds) {
    final double[] vec = new double[bounds.getDimensionality()];
    for(int i = 0; i < bounds.getDimensionality(); i++) {
      vec[i] = ( bounds.getMax(i) - bounds.getMin(i) ) / 2.0 + bounds.getMin(i);
    }
    return new DoubleVector(vec);
  }
}
