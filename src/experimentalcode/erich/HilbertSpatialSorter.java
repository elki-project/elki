package experimentalcode.erich;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

public class HilbertSpatialSorter extends AbstractSpatialSorter {
  /**
   * Constructor.
   */
  public HilbertSpatialSorter() {
    super();
  }

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs, int start, int end, double[] minmax) {
    final int dims = minmax.length >>> 1;
    hilbertSort(objs, start, end, minmax, 0, 0, false, BitsUtil.zero(dims), BitsUtil.zero(dims));
  }

  private <T extends SpatialComparable> void hilbertSort(List<T> objs, final int start, final int end, double[] mms, final int depth, final int rotation, boolean inv, long[] coords, long[] reflections) {
    final int dims = mms.length >>> 1;
    // Completed level of hilbert curve?
    final boolean complete = (depth + 1) % dims == 0;
    final int axis = (depth + rotation) % dims;
    inv ^= BitsUtil.get(reflections, axis);

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
    LoggingUtil.warning("Depth: " + depth + (complete ? "!" : " ") + " axis: " + (inv ? "-" : "+") + axis + " refl: " + BitsUtil.toString(reflections) + " coords: " + BitsUtil.toString(coords) + " " + FormatUtil.format(mms));
    int split = pivotizeList1D(objs, start, end, axis + 1, half, inv);
    // Need to descend?
    if(end - split <= 1 && split - start <= 1) {
      return;
    }

    if(complete) {
      if(inv) {
        BitsUtil.flipI(coords, axis);
      }
      if(start < split - 1) {
        int rot = cyclicTrailingZeros(coords, rotation, dims);
        final int nextrot = (rotation - rot + 1 + dims) % dims;
        LoggingUtil.warning("ARotation old: " + rotation + " c: " + BitsUtil.toString(coords) + " ffs: " + rot + " new: " + nextrot);
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, nextrot, false, BitsUtil.zero(dims), reflections);
      }
      BitsUtil.flipI(coords, axis);
      if(rotation > 0) {
        BitsUtil.flipI(reflections, rotation);
      }
      if(split < end - 1) {
        int rot = cyclicTrailingZeros(coords, rotation, dims);
        final int nextrot = (rotation - rot + 1 + dims) % dims;
        LoggingUtil.warning("BRotation old: " + rotation + " c: " + BitsUtil.toString(coords) + " ffs: " + rot + " new: " + nextrot);
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, nextrot, false, BitsUtil.zero(dims), reflections);
      }
      if(!inv) {
        BitsUtil.flipI(coords, axis);
      }
      if(rotation > 0) {
        BitsUtil.flipI(reflections, rotation);
      }
    }
    else {
      if(inv) {
        BitsUtil.flipI(coords, axis);
      }
      if(start < split - 1) {
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, rotation, false, coords, reflections);
      }
      BitsUtil.flipI(coords, axis);
      if(split < end - 1) {
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, rotation, true, coords, reflections);
      }
      if(!inv) {
        BitsUtil.flipI(coords, axis);
      }
    }
    // Restore ranges
    mms[2 * axis] = min;
    mms[2 * axis + 1] = max;
    // FIXME: implement completely and test.
  }

  private static int cyclicTrailingZeros(long[] bitset, int start, int dims) {
    start -= 1;
    int l = BitsUtil.previousSetBit(bitset, start);
    if(l >= 0) {
      return start - l;
    }
    l = BitsUtil.previousSetBit(bitset, dims - 1);
    if(l >= 0) {
      return dims - l + start;
    }
    return -1;
  }
}