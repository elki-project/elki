package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Derivative Dynamic Time Warping distance for numerical vectors.
 * 
 * Reference:
 * <p>
 * E. J. Keogh and M. J. Pazzani<br />
 * Derivative dynamic time warping<br />
 * In the 1st SIAM International Conference on Data Mining (SDM-2001), Chicago,
 * IL, USA.
 * </p>
 * 
 * @author Lara Hirschbeck, Daniel Kolb
 * @since 0.2
 */
@Title("Derivative dynamic time warping")
@Reference(authors = "E. J. Keogh and M. J. Pazzani", //
title = "Derivative dynamic time warping", //
booktitle = "1st SIAM International Conference on Data Mining (SDM-2001)", //
url = "https://siam.org/proceedings/datamining/2001/dm01_01KeoghE.pdf")
public class DerivativeDTWDistanceFunction extends DTWDistanceFunction {
  /**
   * Constructor.
   */
  public DerivativeDTWDistanceFunction() {
    this(Double.POSITIVE_INFINITY);
  }

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   */
  public DerivativeDTWDistanceFunction(double bandSize) {
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
      final double val1 = derivative(i, v1);
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
        buf[nxt + j] = min + delta(val1, derivative(j, v2));
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
    return Math.sqrt(buf[cur + dim2 - 1]);
  }

  @Override
  protected void firstRow(double[] buf, int band, NumberVector v1, NumberVector v2, int dim2) {
    // First cell:
    final double val1 = derivative(0, v1);
    buf[0] = delta(val1, derivative(0, v2));

    // Width of valid area:
    final int w = (band >= dim2) ? dim2 - 1 : band;
    // Fill remaining part of buffer:
    for(int j = 1; j <= w; j++) {
      buf[j] = buf[j - 1] + delta(val1, derivative(j, v2));
    }
  }

  /**
   * Given a NumberVector and the position of an element, approximates the
   * gradient of given element.
   * 
   * @return Derivative as double
   */
  public double derivative(int i, NumberVector v) {
    final int dim = v.getDimensionality();
    if(dim == 1) {
      return 0.;
    }
    // Adjust for boundary conditions, as per the article:
    i = (i == 0) ? 1 : (i == dim - 1) ? dim - 2 : i;
    return (v.doubleValue(i) - v.doubleValue(i - 1) + (v.doubleValue(i + 1) - v.doubleValue(i - 1)) * .5) * .5;
  }
}