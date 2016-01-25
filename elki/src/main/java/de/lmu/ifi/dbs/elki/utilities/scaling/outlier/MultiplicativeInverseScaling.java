package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Scaling function to invert values basically by computing 1/x, but in a variation
 * that maps the values to the [0:1] interval and avoiding division by 0.
 * 
 * The exact formula can be written as
 * 
 * 1 / (value * max_{x!=0}(1 / abs(x))) = min_{x != 0}(abs(x)) / x
 * 
 * with 1 / 0 := 1
 * 
 * Reference:
 * <p>
 * H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek<br />
 * Interpreting and Unifying Outlier Scores<br />
 * Proc. 11th SIAM International Conference on Data Mining (SDM), Mesa, AZ, 2011
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", //
title = "Interpreting and Unifying Outlier Scores", //
booktitle = "Proc. 11th SIAM International Conference on Data Mining (SDM), Mesa, AZ, 2011", //
url = "http://dx.doi.org/10.1137/1.9781611972818.2")
public class MultiplicativeInverseScaling implements OutlierScalingFunction {
  /**
   * Constructor.
   */
  public MultiplicativeInverseScaling() {
    super();
  }

  /**
   * Scaling value, set by {@link #prepare}.
   */
  double scaleval = 0;

  @Override
  public double getScaled(double value) {
    assert (scaleval != 0) : "prepare() was not run prior to using the scaling function.";
    try {
      return 1.0 / (value * scaleval);
    }
    catch(ArithmeticException e) {
      return 1.0;
    }
  }

  @Override
  public void prepare(OutlierResult or) {
    double max = Double.MIN_VALUE;
    DoubleRelation scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      double val = scores.doubleValue(id);
      double inv = Math.abs(1.0 / val);
      if(!Double.isInfinite(inv) && !Double.isNaN(inv)) {
        max = Math.max(max, inv);
      }
    }
    scaleval = max;
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    double max = Double.MIN_VALUE;
    final int size = adapter.size(array);
    for (int i = 0; i < size; i++) {
      double inv = Math.abs(1.0 / adapter.getDouble(array, i));
      if(!Double.isInfinite(inv) && !Double.isNaN(inv)) {
        max = Math.max(max, inv);
      }
    }
    scaleval = max;
  }

  @Override
  public double getMin() {
    return 0.0;
  }
  
  @Override
  public double getMax() {
    return 1.0;
  }
}