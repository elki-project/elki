package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

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
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

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
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter, final int len) {
    if(len < 2) {
      return 0.;
    }
    double hsum = 0.;
    double sum = Math.log(adapter.getDouble(data, 0));
    for(int i = 1; i < len; i++) {
      // The next observation:
      final double v = adapter.getDouble(data, i);
      assert (v > 0);
      final double logv = Math.log(v);
      // Aggregate hill estimations:
      hsum += (sum / i - logv);
      // Update sum for next hill.
      sum += logv;
    }
    return -(len) / hsum;
  }
}
