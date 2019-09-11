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
package elki.distance.timeseries;

import elki.data.NumberVector;
import elki.data.type.VectorTypeInformation;
import elki.distance.AbstractNumberVectorDistance;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Longest Common Subsequence distance for numerical vectors.
 * <p>
 * Originally this was based on the Matlab Code by Michalis Vlachos, but we have
 * since switched to a version that uses less memory.
 * <p>
 * Reference:
 * <p>
 * M. Vlachos, M. Hadjieleftheriou, D. Gunopulos, E. Keogh<br>
 * Indexing Multi-Dimensional Time-Series with Support for Multiple Distance
 * Measures<br>
 * Proc. 9th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining
 *
 * @author Thomas Bernecker
 * @since 0.2
 */
@Title("Longest Common Subsequence distance function")
@Reference(authors = "M. Vlachos, M. Hadjieleftheriou, D. Gunopulos, E. Keogh", //
    title = "Indexing Multi-Dimensional Time-Series with Support for Multiple Distance Measures", //
    booktitle = "Proc. 9th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1145/956750.956777", //
    bibkey = "DBLP:conf/kdd/VlachosHGK03")
public class LCSSDistance extends AbstractNumberVectorDistance {
  /**
   * Keeps the currently set pDelta.
   */
  private double pDelta;

  /**
   * Keeps the currently set pEpsilon.
   */
  private double pEpsilon;

  /**
   * Constructor.
   * 
   * @param pDelta pDelta
   * @param pEpsilon pEpsilon
   */
  public LCSSDistance(double pDelta, double pEpsilon) {
    super();
    this.pDelta = pDelta;
    this.pEpsilon = pEpsilon;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    if(dim1 > dim2) {
      return distance(v2, v1);
    }
    final int delta = (int) Math.ceil(dim2 * pDelta);

    // Compute value range, for scaling epsilon:
    final double epsilon = getRange(v1, dim1, v2, dim2) * pEpsilon;

    double[] curr = new double[dim2 + 1], next = new double[dim2 + 1];

    for(int i = 0; i < dim1; i++) {
      final double ai = v1.doubleValue(i);
      for(int j = Math.max(0, i - delta); j <= Math.min(dim2 - 1, i + delta); j++) {
        final double bj = v2.doubleValue(j);
        if((bj + epsilon) >= ai && (bj - epsilon) <= ai) { // match
          next[j + 1] = curr[j] + 1;
        }
        else if(curr[j + 1] > next[j]) { // ins
          next[j + 1] = curr[j + 1];
        }
        else { // del
          next[j + 1] = next[j];
        }
      }
      // Swap
      double[] tmp = curr;
      curr = next;
      next = tmp;
    }

    // search for maximum in the last line
    double maxEntry = curr[1];
    for(int i = 2; i < dim2 + 1; i++) {
      maxEntry = (curr[i] > maxEntry) ? curr[i] : maxEntry;
    }
    final double sim = maxEntry / Math.min(dim1, dim2);
    return 1. - sim;
  }

  public double getRange(NumberVector v1, final int dim1, NumberVector v2, final int dim2) {
    double min = v1.doubleValue(0), max = min;
    for(int i = 1; i < dim1; i++) {
      final double v = v1.doubleValue(i);
      min = (v < min) ? v : min;
      max = (v > max) ? v : max;
    }
    for(int i = 0; i < dim2; i++) {
      final double v = v2.doubleValue(i);
      min = (v < min) ? v : min;
      max = (v > max) ? v : max;
    }
    final double range = max - min;
    return range;
  }

  @Override
  public VectorTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.VARIABLE_LENGTH;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()) //
        && this.pDelta == ((LCSSDistance) obj).pDelta //
        && this.pEpsilon == ((LCSSDistance) obj).pEpsilon);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() ^ (Double.hashCode(pDelta) * 31 + Double.hashCode(pEpsilon));
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * PDELTA parameter
     */
    public static final OptionID PDELTA_ID = new OptionID("lcss.pDelta", "the allowed deviation in x direction for LCSS alignment (positive double value, 0 <= pDelta <= 1)");

    /**
     * PEPSILON parameter
     */
    public static final OptionID PEPSILON_ID = new OptionID("lcss.pEpsilon", "the allowed deviation in y direction for LCSS alignment (positive double value, 0 <= pEpsilon <= 1)");

    /**
     * Keeps the currently set pDelta.
     */
    private double pDelta;

    /**
     * Keeps the currently set pEpsilon.
     */
    private double pEpsilon;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(PDELTA_ID, 0.1)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE)//
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> pDelta = x);
      new DoubleParameter(PEPSILON_ID, 0.05)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> pEpsilon = x);
    }

    @Override
    public LCSSDistance make() {
      return new LCSSDistance(pDelta, pEpsilon);
    }
  }
}
