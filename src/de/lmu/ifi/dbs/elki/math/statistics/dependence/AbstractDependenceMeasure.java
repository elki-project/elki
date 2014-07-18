package de.lmu.ifi.dbs.elki.math.statistics.dependence;

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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for dependence measures.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractDependenceMeasure implements DependenceMeasure {
  @Override
  public double dependence(double[] data1, double[] data2) {
    return dependence(ArrayLikeUtil.DOUBLEARRAYADAPTER, data1, ArrayLikeUtil.DOUBLEARRAYADAPTER, data2);
  }

  @Override
  public <A> double[] dependence(NumberArrayAdapter<?, A> adapter, ArrayList<A> data) {
    final int dims = data.size();
    double[] out = new double[(dims * (dims - 1)) >> 1];
    int o = 0;
    for(int y = 1; y < dims; y++) {
      A dy = data.get(y);
      for(int x = 0; x < y; x++) {
        out[o++] = dependence(adapter, data.get(x), adapter, dy);
      }
    }
    return out;
  }

  /**
   * Index into the serialized array.
   * 
   * @param x Column
   * @param y Row
   * @return Index in serialized array
   */
  protected static int index(int x, int y) {
    assert (x < y) : "Only lower triangle is allowed.";
    return ((y * (y - 1)) >> 1) + x;
  }

  /**
   * Validate the length of the two data sets (must be the same, and non-zero)
   * 
   * @param adapter1 First data adapter
   * @param data1 First data set
   * @param adapter2 Second data adapter
   * @param data2 Second data set
   * @param <A> First array type
   * @param <B> Second array type
   */
  public static <B, A> int size(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = adapter1.size(data1);
    if(len != adapter2.size(data2)) {
      throw new AbortException("Array sizes do not match!");
    }
    if(len == 0) {
      throw new AbortException("Empty array!");
    }
    return len;
  }
}
