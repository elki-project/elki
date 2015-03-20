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
 * Hill estimator of the intrinsic dimensionality.
 * 
 * Reference:
 * <p>
 * Hill, B. M.<br />
 * A simple general approach to inference about the tail of a distribution<br />
 * The annals of statistics, 3(5), 1163-1174
 * </p>
 * 
 * @author Jonathan von Brünken
 * @author Erich Schubert
 */
@Reference(authors = "Hill, B. M.", //
title = "A simple general approach to inference about the tail of a distribution", //
booktitle = "The annals of statistics, 3(5), 1163-1174", //
url = "http://dx.doi.org/10.1214/aos/1176343247")
public class HillEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final HillEstimator STATIC = new HillEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int n = adapter.size(data);
    if(n < 2) {
      return 0.;
    }
    double sum = 0.;
    for(int i = 0; i < n - 1; ++i) {
      double v = adapter.getDouble(data, i);
      assert (v > 0.);
      sum += Math.log(v);
    }
    sum /= (n - 1);
    sum -= Math.log(adapter.getDouble(data, n - 1));
    return -1. / sum;
  }
}
