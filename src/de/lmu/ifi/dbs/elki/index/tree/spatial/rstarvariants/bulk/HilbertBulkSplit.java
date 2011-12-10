package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk;

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
import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

public class HilbertBulkSplit extends AbstractBulkSplit {
  /**
   * Constructor.
   */
  public HilbertBulkSplit() {
    super();
  }

  @Override
  public <T extends SpatialComparable> List<List<T>> partition(List<T> objs, int minEntries, int maxEntries) {
    final int dim = objs.get(0).getDimensionality();
    // Find the mean for splitting.
    double[] mm = new double[dim * 2];
    {
      for(int i = 0; i < dim; i++) {
        mm[i * 2] = Double.POSITIVE_INFINITY;
        mm[i * 2 + 1] = Double.NEGATIVE_INFINITY;
      }
      for(SpatialComparable obj : objs) {
        for(int d = 1; d <= dim; d++) {
          mm[2 * d - 2] = Math.min(mm[2 * d - 2], obj.getMin(d));
          mm[2 * d - 1] = Math.max(mm[2 * d - 1], obj.getMax(d));
        }
      }
    }

    hilbertSort(objs, 0, objs.size(), mm, new BitSet(dim), 1, false);
    return trivialPartition(objs, minEntries, maxEntries);
  }

  protected <T extends SpatialComparable> void hilbertSort(List<T> objs, int start, int end, double[] mms, BitSet high, int dim, boolean desc) {
    if(start >= end - 1) {
      return;
    }
    // LoggingUtil.warning(FormatUtil.format(mms));
    // Find the mean for splitting.
    double tm = (mms[2 * dim - 2] + mms[2 * dim - 1]);
    // Split the data set on the mean via partial resorting
    int s = start, e = end - 1;
    while(s < e) {
      double stm = getMinMaxObject(objs, s, dim);
      while((stm <= tm) != desc && s < e) {
        s++;
        stm = getMinMaxObject(objs, s, dim);
      }
      double etm = getMinMaxObject(objs, e, dim);
      while((etm >= tm) != desc && s < e) {
        e--;
        etm = getMinMaxObject(objs, e, dim);
      }
      if(s >= e) {
        assert (s == e);
        break;
      }
      // Swap
      T temp = objs.get(e);
      objs.set(e, objs.get(s));
      objs.set(s, temp);
      s++;
      e--;
    }
    // LoggingUtil.warning("start: " + start + " end: " + end + " s: " + s);
    int nextdim = (dim % objs.get(0).getDimensionality()) + 1;
    if(nextdim == 1) {
      double[] subset = new double[2 * dim];
      for(int d = 0; d < dim - 1; d++) {
        if(high.get(d)) {
          subset[2 * d] = (mms[2 * d] + mms[2 * d + 1]) / 2;
          subset[2 * d + 1] = mms[2 * d + 1];
        }
        else {
          subset[2 * d] = mms[2 * d];
          subset[2 * d + 1] = (mms[2 * d] + mms[2 * d + 1]) / 2;
        }
      }
      if(s < end - 1) {
        int d = dim - 1;
        subset[2 * d] = (mms[2 * d] + mms[2 * d + 1]) / 2;
        subset[2 * d + 1] = mms[2 * d + 1];
        hilbertSort(objs, s, end, subset, new BitSet(), nextdim, !desc);
      }
      if(start < s - 1) {
        int d = dim - 1;
        subset[2 * d] = mms[2 * d];
        subset[2 * d + 1] = (mms[2 * d] + mms[2 * d + 1]) / 2;
        hilbertSort(objs, start, s, subset, new BitSet(), nextdim, !desc);
      }
    }
    else {
      if(s < end - 1) {
        high.set(dim - 1);
        hilbertSort(objs, s, end, mms, high, nextdim, desc);
        high.clear(dim - 1);
      }
      if(start < s - 1) {
        hilbertSort(objs, start, s, mms, high, nextdim, desc);
      }
    }
    // FIXME: implement completely and test.
  }

  protected double getMinMaxObject(List<? extends SpatialComparable> objs, int s, int dim) {
    SpatialComparable sobj = objs.get(s);
    double stm = sobj.getMin(dim) + sobj.getMax(dim);
    return stm;
  }
}
