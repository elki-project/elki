package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Normalization used by HeDES
 * 
 * @author Erich Schubert
 */
@Reference(authors = "H. V. Nguyen, H. H. Ang, V. Gopalkrishnan", title = "Mining Outliers with Ensemble of Heterogeneous Detectors on Random Subspaces", booktitle = "Proc. 15th International Conference on Database Systems for Advanced Applications (DASFAA 2010)", url = "http://dx.doi.org/10.1007/978-3-642-12026-8_29")
public class HeDESNormalizationOutlierScaling implements OutlierScalingFunction {
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

    for(DBID id : or.getScores().iterDBIDs()) {
      double val = or.getScores().get(id);
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
    return (value - mean) / stddev;
  }
}