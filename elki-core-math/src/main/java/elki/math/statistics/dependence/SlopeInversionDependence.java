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
package elki.math.statistics.dependence;

import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import net.jafama.FastMath;

/**
 * Arrange dimensions based on the entropy of the slope spectrum.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:<br>
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees.<br>
 * Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)
 * <p>
 * TODO: shouldn't this be normalized by the single-dimension entropies or so?
 * 
 * @author Erich Schubert
 * @author Robert Rödler
 * @since 0.5.5
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", //
    booktitle = "Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)", //
    url = "https://doi.org/10.1145/2463676.2463696", //
    bibkey = "DBLP:conf/sigmod/AchtertKSZ13")
public class SlopeInversionDependence extends SlopeDependence {
  /**
   * Static instance.
   */
  public static final SlopeInversionDependence STATIC = new SlopeInversionDependence();

  /**
   * Constructor. Use static instance instead!
   */
  protected SlopeInversionDependence() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = Util.size(adapter1, data1, adapter2, data2);

    // Get attribute value range:
    final double off1, scale1, off2, scale2;
    {
      double mi = adapter1.getDouble(data1, 0), ma = mi;
      for(int i = 1; i < len; ++i) {
        double v = adapter1.getDouble(data1, i);
        mi = v < mi ? v : mi;
        ma = v > ma ? v : ma;
      }
      off1 = mi;
      scale1 = (ma > mi) ? (1. / (ma - mi)) : 1.;
      // Second data
      mi = ma = adapter2.getDouble(data2, 0);
      for(int i = 1; i < len; ++i) {
        double v = adapter2.getDouble(data2, i);
        mi = v < mi ? v : mi;
        ma = v > ma ? v : ma;
      }
      off2 = mi;
      scale2 = (ma > mi) ? (1. / (ma - mi)) : 1.;
    }

    // Collect angular histograms.
    // Note, we only fill half of the matrix
    int[] angles = new int[PRECISION];
    int[] angleI = new int[PRECISION];

    // Scratch buffer
    for(int i = 0; i < len; i++) {
      double x = adapter1.getDouble(data1, i), y = adapter2.getDouble(data2, i);
      x = (x - off1) * scale1;
      y = (y - off2) * scale2;
      {
        final double delta = x - y + 1;
        int div = (int) Math.round(delta * RESCALE);
        // TODO: do we really need this check?
        div = (div < 0) ? 0 : (div >= PRECISION) ? PRECISION - 1 : div;
        angles[div] += 1;
      }
      {
        final double delta = x + y;
        int div = (int) Math.round(delta * RESCALE);
        // TODO: do we really need this check?
        div = (div < 0) ? 0 : (div >= PRECISION) ? PRECISION - 1 : div;
        angleI[div] += 1;
      }
    }

    // Compute entropy:
    double entropy = 0., entropyI = 0.;
    for(int l = 0; l < PRECISION; l++) {
      if(angles[l] > 0) {
        final double p = angles[l] / (double) len;
        entropy += p * FastMath.log(p);
      }
      if(angleI[l] > 0) {
        final double p = angleI[l] / (double) len;
        entropyI += p * FastMath.log(p);
      }
    }
    if(entropy >= entropyI) {
      return 1 + entropy / LOG_PRECISION;
    }
    else {
      return 1 + entropyI / LOG_PRECISION;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public SlopeInversionDependence make() {
      return STATIC;
    }
  }
}
