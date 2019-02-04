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
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Normalization used by HeDES
 * <p>
 * Reference:<br>
 * H. V. Nguyen, H. H. Ang, V. Gopalkrishnan<br>
 * Mining Outliers with Ensemble of Heterogeneous Detectors on Random
 * Subspaces<br>
 * Proc. 15th Int. Conf. Database Systems for Advanced Applications (DASFAA
 * 2010)
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Reference(authors = "H. V. Nguyen, H. H. Ang, V. Gopalkrishnan", //
    title = "Mining Outliers with Ensemble of Heterogeneous Detectors on Random Subspaces", //
    booktitle = "Proc. 15th Int. Conf. Database Systems for Advanced Applications (DASFAA 2010)", //
    url = "https://doi.org/10.1007/978-3-642-12026-8_29", //
    bibkey = "DBLP:conf/dasfaa/VuAG10")
public class HeDESNormalizationOutlierScaling implements OutlierScaling {
  /**
   * Mean
   */
  double mean;

  /**
   * Standard deviation
   */
  double stddev;

  /**
   * Minimum after scaling
   */
  double scaledmin;

  /**
   * Maximum after scaling
   */
  double scaledmax;

  @Override
  public void prepare(OutlierResult or) {
    MeanVariance mv = new MeanVariance();
    DoubleMinMax minmax = new DoubleMinMax();

    DoubleRelation scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      double val = scores.doubleValue(id);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mv.put(val);
        minmax.put(val);
      }
    }

    mean = mv.getMean();
    stddev = mv.getSampleStddev();
    scaledmax = getScaled(minmax.getMax());
    scaledmin = getScaled(minmax.getMin());
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    MeanVariance mv = new MeanVariance();
    DoubleMinMax minmax = new DoubleMinMax();

    final int size = adapter.size(array);
    for(int i = 0; i < size; i++) {
      double val = adapter.getDouble(array, i);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mv.put(val);
        minmax.put(val);
      }
    }

    mean = mv.getMean();
    stddev = mv.getSampleStddev();
    scaledmax = getScaled(minmax.getMax());
    scaledmin = getScaled(minmax.getMin());
  }

  @Override
  public double getMax() {
    return scaledmax;
  }

  @Override
  public double getMin() {
    return scaledmin;
  }

  @Override
  public double getScaled(double value) {
    assert (stddev > 0 || (value == mean)) : "prepare() was not run prior to using the scaling function.";
    if(stddev > 0.) {
      return (value - mean) / stddev;
    }
    else {
      return 0.;
    }
  }
}
