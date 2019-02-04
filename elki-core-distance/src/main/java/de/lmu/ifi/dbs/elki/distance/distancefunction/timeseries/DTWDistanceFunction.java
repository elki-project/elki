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
package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import net.jafama.FastMath;

/**
 * Dynamic Time Warping distance (DTW) for numerical vectors.
 * <p>
 * Reference:
 * <p>
 * D. Berndt and J. Clifford<br>
 * Using dynamic time warping to find patterns in time series<br>
 * AAAI-94 Workshop on Knowledge Discovery in Databases, 1994
 * <p>
 * This implementation uses a buffer storing two rows.
 * <p>
 * TODO: allow different one-dimensional distances
 * 
 * @author Thomas Bernecker
 * @author Erich Schubert
 * @since 0.2
 */
@Title("Dynamic Time Warping Distance Function")
@Reference(authors = "D. Berndt, J. Clifford", //
    title = "Using dynamic time warping to find patterns in time series", //
    booktitle = "AAAI-94 Workshop on Knowledge Discovery in Databases, 1994", //
    url = "http://www.aaai.org/Papers/Workshops/1994/WS-94-03/WS94-03-031.pdf", //
    bibkey = "DBLP:conf/kdd/BerndtC94")
public class DTWDistanceFunction extends AbstractEditDistanceFunction {
  /**
   * Constructor.
   */
  public DTWDistanceFunction() {
    this(Double.POSITIVE_INFINITY);
  }

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   */
  public DTWDistanceFunction(double bandSize) {
    super(bandSize);
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    // Dimensionality, and last valid value in second vector:
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int m2 = dim2 - 1;

    // bandsize is the maximum allowed distance to the diagonal
    final int band = effectiveBandSize(dim1, dim2);
    // unsatisfiable - lengths too different!
    if(Math.abs(dim1 - dim2) > band) {
      return Double.POSITIVE_INFINITY;
    }
    // Current and previous columns of the matrix
    double[] buf = new double[dim2 << 1];
    Arrays.fill(buf, Double.POSITIVE_INFINITY);

    // Fill first row:
    firstRow(buf, band, v1, v2, dim2);

    // Active buffer offsets (cur = read, nxt = write)
    int cur = 0, nxt = dim2;
    // Fill remaining rows:
    int i = 1, l = 0, r = Math.min(m2, i + band);
    while(i < dim1) {
      final double val1 = v1.doubleValue(i);
      for(int j = l; j <= r; j++) {
        // Value in previous row (must exist, may be infinite):
        double min = buf[cur + j];
        // Diagonal:
        if(j > 0) {
          final double pij = buf[cur + j - 1];
          min = (pij < min) ? pij : min;
          // Previous in same row:
          if(j > l) {
            final double pj = buf[nxt + j - 1];
            min = (pj < min) ? pj : min;
          }
        }
        // Write:
        buf[nxt + j] = min + delta(val1, v2.doubleValue(j));
      }
      // Swap buffer positions:
      cur = dim2 - cur;
      nxt = dim2 - nxt;
      // Update positions:
      ++i;
      if(i > band) {
        ++l;
      }
      if(r < m2) {
        ++r;
      }
    }

    // TODO: support Euclidean, Manhattan here:
    return FastMath.sqrt(buf[cur + dim2 - 1]);
  }

  /**
   * Fill the first row.
   * 
   * @param buf Buffer
   * @param band Bandwidth
   * @param v1 First vector
   * @param v2 Second vector
   * @param dim2 Dimensionality of second
   */
  protected void firstRow(double[] buf, int band, NumberVector v1, NumberVector v2, int dim2) {
    // First cell:
    final double val1 = v1.doubleValue(0);
    buf[0] = delta(val1, v2.doubleValue(0));

    // Width of valid area:
    final int w = (band >= dim2) ? dim2 - 1 : band;
    // Fill remaining part of buffer:
    for(int j = 1; j <= w; j++) {
      buf[j] = buf[j - 1] + delta(val1, v2.doubleValue(j));
    }
  }

  /**
   * Compute the delta of two values.
   * <p>
   * TODO: support Euclidean, Manhattan, others?
   * 
   * @param val1 First value
   * @param val2 Second value
   * @return Difference
   */
  protected double delta(double val1, double val2) {
    double diff = val1 - val2;
    return diff * diff;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractEditDistanceFunction.Parameterizer {
    @Override
    protected DTWDistanceFunction makeInstance() {
      return new DTWDistanceFunction(bandSize);
    }
  }
}