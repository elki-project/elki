package experimentalcode.erich;

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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.AbstractBulkSplit;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Bulk-load an R-tree index by presorting the objects with their position on
 * the Peano curve.
 * 
 * INCOMPLETE attempt at a divdie & conquer solution. TODO: for small sets,
 * resort to different strategy? Dupe handling? Fixme: Orientations
 * 
 * @author Erich Schubert
 */
public class PeanoBulkSplit extends AbstractBulkSplit {
  /**
   * Constructor.
   */
  public PeanoBulkSplit() {
    super();
  }

  @Override
  public <T extends SpatialComparable> List<List<T>> partition(List<T> objs, int minEntries, int maxEntries) {
    final int dim = objs.get(0).getDimensionality();
    // Compute min and max for each dimension:
    double[] mm = new double[dim * 2];
    {
      for(int i = 0; i < dim; i++) {
        mm[i * 2] = Double.POSITIVE_INFINITY;
        mm[i * 2 + 1] = Double.NEGATIVE_INFINITY;
      }
      for(SpatialComparable obj : objs) {
        for(int d = 0; d < dim; d++) {
          mm[2 * d] = Math.min(mm[2 * d], obj.getMin(d + 1));
          mm[2 * d + 1] = Math.max(mm[2 * d + 1], obj.getMax(d + 1));
        }
      }
    }

    peanoSort(objs, 0, objs.size(), mm, 0, false);
    return trivialPartition(objs, minEntries, maxEntries);
  }

  protected <T extends SpatialComparable> void peanoSort(List<T> objs, int start, int end, double[] mms, int dim, boolean desc) {
    // Find the splitting points.
    final double min = mms[2 * dim], max = mms[2 * dim + 1];
    // Safeguard against duplicate points:
    if(max - min < 1E-10) {
      boolean ok = false;
      for(int d = 0; d < mms.length; d += 2) {
        if(mms[d + 1] - mms[d] >= 1E-10) {
          LoggingUtil.warning("No: " + (mms[d + 1] - mms[d]));
          ok = true;
          break;
        }
      }
      if(!ok) {
        return;
      }
    }
    double tfirst = (min + min + max) / 3.;
    double tsecond = (min + max + max) / 3.;
    // Split the data set into three parts
    // LoggingUtil.warning("dim: " + dim + " " + min + "<" + tfirst + "<" +
    // tsecond + "<" + max);
    int fsplit = splitSort(objs, start, end, dim + 1, 2 * tfirst, desc);
    int ssplit = (fsplit < end - 1) ? splitSort(objs, fsplit, end, dim + 1, 2 * tsecond, desc) : fsplit;
    // LoggingUtil.warning("start: " + start + " end: " + end + " s: " + fsplit
    // + ", " + ssplit);
    int nextdim = (dim + 1) % objs.get(0).getDimensionality();
    // Do we need to update the min/max values?
    if(start < fsplit - 1) {
      mms[2 * dim] = min;
      mms[2 * dim + 1] = tfirst;
      peanoSort(objs, start, fsplit, mms, nextdim, desc);
    }
    if(fsplit < ssplit - 1) {
      mms[2 * dim] = tfirst;
      mms[2 * dim + 1] = tsecond;
      peanoSort(objs, fsplit, ssplit, mms, nextdim, !desc);
    }
    if(ssplit < end - 1) {
      mms[2 * dim] = tsecond;
      mms[2 * dim + 1] = max;
      peanoSort(objs, ssplit, end, mms, nextdim, desc);
    }
    // Restore
    mms[2 * dim] = min;
    mms[2 * dim + 1] = max;
    // FIXME: implement completely and test.
  }

  protected <T extends SpatialComparable> int splitSort(List<T> objs, int start, int end, int dim, double tfirst, boolean desc) {
    int s = start, e = end;
    while(s < e) {
      double stm = getMinMaxObject(objs, s, dim);
      while((stm <= tfirst) != desc && s + 1 < e) {
        s++;
        stm = getMinMaxObject(objs, s, dim);
      }
      double etm = getMinMaxObject(objs, e - 1, dim);
      while((etm >= tfirst) != desc && s < e - 1) {
        e--;
        etm = getMinMaxObject(objs, e - 1, dim);
      }
      if(s >= e) {
        assert (s == e);
        break;
      }
      // Swap
      objs.set(s, objs.set(e - 1, objs.get(s)));
      s++;
      e--;
    }
    return s;
  }

  protected double getMinMaxObject(List<? extends SpatialComparable> objs, int s, int dim) {
    SpatialComparable sobj = objs.get(s);
    double stm = sobj.getMin(dim) + sobj.getMax(dim);
    return stm;
  }
}
