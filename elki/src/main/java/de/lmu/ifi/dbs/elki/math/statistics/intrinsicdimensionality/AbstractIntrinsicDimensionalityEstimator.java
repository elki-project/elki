package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

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

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Abstract base class for ID estimators.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractIntrinsicDimensionalityEstimator implements IntrinsicDimensionalityEstimator {
  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter) {
    return estimate(data, adapter, adapter.size(data));
  }

  @Override
  public double estimate(double[] distances) {
    return estimate(distances, ArrayLikeUtil.DOUBLEARRAYADAPTER, distances.length);
  }

  @Override
  public double estimate(double[] distances, int size) {
    return estimate(distances, ArrayLikeUtil.DOUBLEARRAYADAPTER, size);
  }
}
