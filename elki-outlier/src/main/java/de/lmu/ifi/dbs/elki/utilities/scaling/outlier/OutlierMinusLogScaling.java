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
package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Scaling function to invert values by computing -log(x)
 * <p>
 * Useful for example for scaling
 * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.anglebased.ABOD}, but see
 * {@link MinusLogStandardDeviationScaling} and {@link MinusLogGammaScaling} for
 * more advanced scalings for this algorithm.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br>
 * Interpreting and Unifying Outlier Scores<br>
 * Proc. 11th SIAM International Conference on Data Mining (SDM 2011)
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Interpreting and Unifying Outlier Scores", //
    booktitle = "Proc. 11th SIAM International Conference on Data Mining (SDM 2011)", //
    url = "https://doi.org/10.1137/1.9781611972818.2", //
    bibkey = "DBLP:conf/sdm/KriegelKSZ11")
public class OutlierMinusLogScaling implements OutlierScaling {
  /**
   * Maximum value seen, set by {@link #prepare}
   */
  double max = 0.0;

  /**
   * Maximum -log value seen, set by {@link #prepare}
   */
  double mlogmax;

  /**
   * Constructor.
   */
  public OutlierMinusLogScaling() {
    super();
  }

  @Override
  public double getScaled(double value) {
    assert (max != 0) : "prepare() was not run prior to using the scaling function.";
    return -FastMath.log(value / max) / mlogmax;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  @Override
  public void prepare(OutlierResult or) {
    DoubleMinMax mm = new DoubleMinMax();
    DoubleRelation scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      double val = scores.doubleValue(id);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mm.put(val);
      }
    }
    max = mm.getMax();
    mlogmax = -FastMath.log(mm.getMin() / max);
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    DoubleMinMax mm = new DoubleMinMax();
    final int size = adapter.size(array);
    for(int i = 0; i < size; i++) {
      double val = adapter.getDouble(array, i);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mm.put(val);
      }
    }
    max = mm.getMax();
    mlogmax = -FastMath.log(mm.getMin() / max);
  }
}
