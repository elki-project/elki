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

import java.util.Arrays;

import elki.data.NumberVector;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Edit Distance With Real Penalty distance for numerical vectors.
 * <p>
 * Reference:
 * <p>
 * L. Chen and R. Ng<br>
 * On the marriage of Lp-norms and edit distance<br>
 * Proc. 13th Int. Conf. on Very Large Data Bases (VLDB '04)
 *
 * @author Thomas Bernecker
 * @since 0.2
 */
@Title("Edit Distance with Real Penalty")
@Reference(authors = "L. Chen, R. Ng", //
    title = "On the marriage of Lp-norms and edit distance", //
    booktitle = "Proc. 13th Int. Conf. on Very Large Data Bases (VLDB '04)", //
    url = "http://www.vldb.org/conf/2004/RS21P2.PDF", //
    bibkey = "DBLP:conf/vldb/ChenN04")
public class ERPDistance extends DTWDistance {
  /**
   * Gap value.
   */
  private final double g;

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   * @param g G parameter
   */
  public ERPDistance(double bandSize, double g) {
    super(bandSize);
    this.g = g;
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
        double min = buf[cur + j] + delta(val1, g);
        // Diagonal:
        if(j > 0) {
          final double pij = buf[cur + j - 1] + delta(val1, v2.doubleValue(j));
          min = (pij < min) ? pij : min;
          // Previous in same row:
          if(j > l) {
            final double pj = buf[nxt + j - 1] + delta(g, v2.doubleValue(j));
            min = (pj < min) ? pj : min;
          }
        }
        // Write:
        buf[nxt + j] = min;
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
    final double val1 = v1.doubleValue(0);
    buf[0] = Math.min(delta(val1, g), delta(val1, v2.doubleValue(0)));

    // Width of valid area:
    final int w = (band >= dim2) ? dim2 - 1 : band;
    // Fill remaining part of buffer:
    for(int j = 1; j <= w; j++) {
      buf[j] = Math.min(delta(val1, g), buf[j - 1]) + delta(val1, v2.doubleValue(j));
    }
  }

  @Override
  protected double delta(double val1, double val2) {
    double diff = val1 - val2;
    return diff * diff;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj) && this.g == ((ERPDistance) obj).g;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + Double.hashCode(g);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par extends AbstractEditDistance.Par {
    /**
     * G parameter
     */
    public static final OptionID G_ID = new OptionID("erp.g", "The g parameter of ERP - comparison value to use in gaps.");

    /**
     * Gap value.
     */
    protected double g = 0.;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(G_ID, 0.) //
          .grab(config, x -> g = x);
    }

    @Override
    public ERPDistance make() {
      return new ERPDistance(bandSize, g);
    }
  }
}
