package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Provides the Derivative Dynamic Time Warping distance for number vectors.
 * 
 * Reference:
 * <p>
 * E. J. Keogh and M. J. Pazzani< br />
 * Derivative dynamic time warping<br />
 * In the 1st SIAM International Conference on Data Mining (SDM-2001), Chicago,
 * IL, USA.
 * </p>
 * 
 * @author Lara Hirschbeck, Daniel Kolb
 */
@Title("Derivative dynamic time warping")
@Reference(authors = "E. J. Keogh and M. J. Pazzani", title = "Derivative dynamic time warping", booktitle = "1st SIAM International Conference on Data Mining (SDM-2001)", url = "https://siam.org/proceedings/datamining/2001/dm01_01KeoghE.pdf")
public class DerivativeDTWDistanceFunction extends AbstractEditDistanceFunction {
  /**
   * Constructor.
   * 
   * @param bandSize Band size
   */
  public DerivativeDTWDistanceFunction(double bandSize) {
    super(bandSize);
  }

  /**
   * Provides the Derivative Dynamic Time Warping distance between the given two
   * vectors.
   * 
   * @return the Derivative Dynamic Time Warping distance between the given two
   *         vectors as an instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    // Current and previous columns of the matrix
    double[] curr = new double[dim2];
    double[] prev = new double[dim2];

    // size of edit distance band
    int band = (int) Math.ceil(dim2 * bandSize);
    // bandsize is the maximum allowed distance to the diagonal

    for(int i = 0; i < dim1; i++) {
      // Swap current and prev arrays. We'll just overwrite the new curr.
      {
        double[] temp = prev;
        prev = curr;
        curr = temp;
      }
      int l = i - (band + 1);
      if(l < 0) {
        l = 0;
      }
      int r = i + (band + 1);
      if(r > (dim2 - 1)) {
        r = (dim2 - 1);
      }

      for(int j = l; j <= r; j++) {
        if(Math.abs(i - j) <= band) {
          final double val1 = derivative(i, v1);
          final double val2 = derivative(j, v2);
          final double diff = (val1 - val2);
          // Formally: diff = Math.sqrt(diff * diff);

          double cost = diff * diff;

          if((i + j) != 0) {
            if((i == 0) || ((j != 0) && ((prev[j - 1] > curr[j - 1]) && (curr[j - 1] < prev[j])))) {
              // del
              cost += curr[j - 1];
            }
            else if((j == 0) || ((i != 0) && ((prev[j - 1] > prev[j]) && (prev[j] < curr[j - 1])))) {
              // ins
              cost += prev[j];
            }
            else {
              // match
              cost += prev[j - 1];
            }
          }

          curr[j] = cost;
        }
        else {
          curr[j] = Double.POSITIVE_INFINITY; // outside band
        }
      }
    }

    return Math.sqrt(curr[dim2 - 1]);
  }

  /**
   * Given a NumberVector and the position of an element, approximates the
   * gradient of given element.
   * 
   * @return Derivative as double
   */
  public double derivative(int i, NumberVector v) {
    final int dim = v.getDimensionality();
    if(dim <= 3) {
      throw new IllegalArgumentException("Derivative DTW: Vector Dimensionality too small for doubleDerivative");
    }
    if(i == 0) {
      return derivative(1, v);
    }
    if(i == dim - 1) {
      return derivative(dim - 2, v);
    }
    return (v.doubleValue(i) - v.doubleValue(i - 1) + (v.doubleValue(i + 1) - v.doubleValue(i - 1)) * .5) * .5;
  }
}