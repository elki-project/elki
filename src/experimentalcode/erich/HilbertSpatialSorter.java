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

public class HilbertSpatialSorter extends AbstractSpatialSorter {
  /**
   * Constructor.
   */
  public HilbertSpatialSorter() {
    super();
  }

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs) {
    double[] mm = computeMinMax(objs);
    hilbertSort(objs, 0, objs.size(), mm, 0, 0, false, false);
  }

  private <T extends SpatialComparable> void hilbertSort(List<T> objs, final int start, final int end, double[] mms, final int depth, final int axis, boolean inv, boolean right) {
    final int dims = mms.length >>> 1;
    final boolean rotate = (depth + 1) % dims == 0;

    // Find the splitting point.
    final double min = mms[2 * axis], max = mms[2 * axis + 1];
    final double half = (min + max) / 2.;
    // Safeguard against duplicate points:
    if(max - half < 1E-10 || half - min < 1E-10) {
      boolean ok = false;
      for(int d = 0; d < mms.length; d += 2) {
        if(mms[d + 1] - mms[d] >= 1E-10) {
          // LoggingUtil.warning("No: " + (mms[d + 1] - mms[d]));
          ok = true;
          break;
        }
      }
      if(!ok) {
        // LoggingUtil.warning("Stop.");
        return;
      }
    }
    int split = pivotizeList1D(objs, start, end, axis + 1, half, inv);

    // LoggingUtil.warning("Depth: " + depth + (rotate ? "r" : "") + " axis: " +
    // (inv ? "-" : "+") + axis + (right ? "R" : "") + " " +
    // FormatUtil.format(mms));
    int nextaxis = (axis + 1) % dims;
    if(!rotate) {
      if(start < split - 1) {
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, nextaxis, inv, false);
      }
      if(split < end - 1) {
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, nextaxis, !inv, true);
      }
    }
    else {
      if(start < split - 1) {
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, !right ? axis : nextaxis, inv ^ right, false);
      }
      if(split < end - 1) {
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, !right ? nextaxis : axis, inv, true);
      }
    }
    // Restore ranges
    mms[2 * axis] = min;
    mms[2 * axis + 1] = max;
    // FIXME: implement completely and test.
  }
}