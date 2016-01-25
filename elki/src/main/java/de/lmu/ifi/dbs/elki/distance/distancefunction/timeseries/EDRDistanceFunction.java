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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Edit Distance on Real Sequence distance for numerical vectors.
 * 
 * Reference:
 * <p>
 * L. Chen and M. T. Özsu and V. Oria<br />
 * Robust and fast similarity search for moving object trajectories<br />
 * SIGMOD '05: Proceedings of the 2005 ACM SIGMOD international conference on
 * Management of data
 * </p>
 * 
 * @author Thomas Bernecker
 * @since 0.2
 */
@Title("Edit Distance on Real Sequence")
@Reference(authors = "L. Chen and M. T. Özsu and V. Oria", //
title = "Robust and fast similarity search for moving object trajectories", //
booktitle = "SIGMOD '05: Proceedings of the 2005 ACM SIGMOD international conference on Management of data", //
url = "http://dx.doi.org/10.1145/1066157.1066213")
public class EDRDistanceFunction extends DTWDistanceFunction {
  /**
   * Delta parameter, similarity threshold for attributes.
   */
  private final double delta;

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   * @param delta Allowed delta
   */
  public EDRDistanceFunction(double bandSize, double delta) {
    super(bandSize);
    this.delta = delta;
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

    return buf[cur + dim2 - 1];
  }

  @Override
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

  @Override
  protected double delta(double val1, double val2) {
    return (Math.abs(val1 - val2) < delta) ? 0. : 1.;
  }

  @Override
  public boolean equals(Object obj) {
    if(!super.equals(obj)) {
      return false;
    }
    return this.delta == ((EDRDistanceFunction) obj).delta;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractEditDistanceFunction.Parameterizer {
    /**
     * DELTA parameter
     */
    public static final OptionID DELTA_ID = new OptionID("edr.delta", "the delta parameter (similarity threshold) for EDR (positive number)");

    /**
     * Delta parameter, similarity threshold for attributes.
     */
    protected double delta = 0.;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter deltaP = new DoubleParameter(DELTA_ID, 1.0) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.doubleValue();
      }
    }

    /**
     * Get parameter delta.
     * 
     * @param config Parameterization
     * @return value
     */
    public static double getParameterDelta(Parameterization config) {
      return 0.0;
    }

    @Override
    protected EDRDistanceFunction makeInstance() {
      return new EDRDistanceFunction(bandSize, delta);
    }
  }
}
