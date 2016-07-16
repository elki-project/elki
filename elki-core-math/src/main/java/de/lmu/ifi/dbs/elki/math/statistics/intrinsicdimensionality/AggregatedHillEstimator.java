package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimator using the weighted average of multiple hill estimators.
 *
 * Reference:
 * <p>
 * R. Huisman and K. G. Koedijk and C. J. M. Kool and F. Palm<br />
 * Tail-Index Estimates in Small Samples<br />
 * Journal of Business & Economic Statistics
 * </p>
 * 
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "R. Huisman and K. G. Koedijk and C. J. M. Kool and F. Palm", //
    title = "Tail-Index Estimates in Small Samples", //
    booktitle = "Journal of Business & Economic Statistics", //
    url = "http://dx.doi.org/10.1198/073500101316970421")
public class AggregatedHillEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final AggregatedHillEstimator STATIC = new AggregatedHillEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    double hsum = 0., sum = 0.;
    int i = 0, valid = 0;
    // First nonzero:
    while(i < end) {
      // The next observation:
      final double v = adapter.getDouble(data, i++);
      if(v > 0) {
        sum = Math.log(v);
        valid++;
        break;
      }
    }
    while(i < end) {
      // The next observation:
      final double v = adapter.getDouble(data, i++);
      assert (v > 0);
      final double logv = Math.log(v);
      // Aggregate hill estimations:
      hsum += sum / valid++ - logv;
      // Update sum for next hill.
      sum += logv;
    }
    if(valid < 1) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    return -valid / hsum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected AggregatedHillEstimator makeInstance() {
      return STATIC;
    }
  }
}
