package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Provides the Dynamic Time Warping distance for FeatureVectors.
 * 
 * @author Thomas Bernecker
 */
@Title("Dynamic Time Warping Distance Function")
@Reference(authors = "Berndt, D. and Clifford, J.", title = "Using dynamic time warping to find patterns in time series", booktitle = "AAAI-94 Workshop on Knowledge Discovery in Databases, 1994", url = "http://www.aaai.org/Papers/Workshops/1994/WS-94-03/WS94-03-031.pdf")
public class DTWDistanceFunction extends AbstractEditDistanceFunction {
  /**
   * Constructor.
   * 
   * @param bandSize Band size
   */
  public DTWDistanceFunction(double bandSize) {
    super(bandSize);
  }

  /**
   * Provides the Dynamic Time Warping distance between the given two vectors.
   * 
   * @return the Dynamic Time Warping distance between the given two vectors as
   *         an instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    // Current and previous columns of the matrix
    double[] curr = new double[v2.getDimensionality()];
    double[] prev = new double[v2.getDimensionality()];

    // size of edit distance band
    int band = (int) Math.ceil(v2.getDimensionality() * bandSize);
    // bandsize is the maximum allowed distance to the diagonal

    // System.out.println("len1: " + features1.length + ", len2: " +
    // features2.length + ", band: " + band);

    for(int i = 0; i < v1.getDimensionality(); i++) {
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
      if(r > (v2.getDimensionality() - 1)) {
        r = (v2.getDimensionality() - 1);
      }

      for(int j = l; j <= r; j++) {
        if(Math.abs(i - j) <= band) {
          double val1 = v1.doubleValue(i + 1);
          double val2 = v2.doubleValue(j + 1);
          double diff = (val1 - val2);
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

    return Math.sqrt(curr[v2.getDimensionality() - 1]);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractEditDistanceFunction.Parameterizer {
    @Override
    protected DTWDistanceFunction makeInstance() {
      return new DTWDistanceFunction(bandSize);
    }
  }
}