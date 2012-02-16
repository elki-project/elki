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

// FIXME: implement completely and test.
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
    final long[] refl = BitsUtil.zero(dims);
    // BitsUtil.flipI(refl, dims - 1);
    boolean inv = (dims == 1);
    hilbertSort(objs, start, end, minmax, 0, 0, refl, BitsUtil.zero(dims), inv, false, dims);
  }

  /**
   * Sort objects according to their hilbert order (without transforming them to
   * their full bit representation!)
   * 
   * @param objs List to sort
   * @param start Sublist start (inclusive)
   * @param end Sublist end (exclusive)
   * @param mms Current MinMax values
   * @param depth Recursion depth
   * @param rotation Current rotation
   * @param refl Reflection bitmask
   * @param hist Raw bits in effect, describing the hypercube
   * @param rev Reversion flag
   * @param gray Gray code carry over for (bits ^ refl)
   * @param last Last bit that was set in (bits ^ refl)
   */
  private <T extends SpatialComparable> void hilbertSort(List<T> objs, final int start, final int end, double[] mms, int depth, final int rotation, long[] refl, long[] hist, boolean rev, boolean gray, int last) {
    // Dimensionality, from minmax array
    final int dims = mms.length >>> 1;
    // Current axis
    final int axis = (rotation + depth) % dims;
    // Current axis reflection bit.
    final boolean is_reflected = BitsUtil.get(refl, axis);
    // Effective sort order. Based on current gray code, preceding gray code,
    // and reversion flag
    final boolean reverse_sort = gray ^ is_reflected ^ rev;

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
    LoggingUtil.warning("Depth: " + depth + " axis: " + (reverse_sort ? "-" : "+") + axis + " refl: " + BitsUtil.toString(refl, dims) + " hist: " + BitsUtil.toString(hist, dims) + " " + (rev ? "r+" : "r-" ) + " " + (gray ? "g+" : "g-") + " " + FormatUtil.format(mms));
    int split = pivotizeList1D(objs, start, end, axis + 1, half, reverse_sort);
    // Need to descend at all?
    if(end - split <= 1 && split - start <= 1) {
      return;
    }

    final int nextdepth = (depth + 1) % dims;
    // Compute array intervals.
    final int lstart, lend, hstart, hend;
    {
      if(!reverse_sort) {
        lstart = start;
        lend = split;
        hstart = split;
        hend = end;
      }
      else {
        hstart = start;
        hend = split;
        lstart = split;
        lend = end;
      }
    }
    if(is_reflected) {
      BitsUtil.flipI(hist, axis);
    }
    // Process "lower" half (if nontrivial)
    if(lend - lstart > 1) {
      updateMinMax(mms, axis, min, half);
      // Compute gray code carry for (hist ^ refl)
      final boolean nextgray = gray ^ is_reflected;
      // Update "first set bit in (hist ^ refl)"
      final int nextlast = is_reflected ? depth : last;
      // LoggingUtil.warning("Low : " + FormatUtil.format(mms) + " nextgray: " +
      // nextgray + " nextfirs: " + nextfirs);

      if(nextdepth > 0) {
        hilbertSort(objs, lstart, lend, mms, nextdepth, rotation, refl, hist, rev, nextgray, nextlast);
      }
      else {
        final int nextrot = (rotation + nextlast + 1) % dims;
        final boolean nrev = rev ^ nextgray;
        LoggingUtil.warning("A old: " + rotation + " r: " + BitsUtil.toString(refl, dims) + " h: " + BitsUtil.toString(hist, dims) + " ffs: " + nextlast + " new: " + nextrot + " " + nrev);
        hilbertSort(objs, lstart, lend, mms, nextdepth, nextrot, hist, BitsUtil.zero(dims), nrev, false, dims);
      }
    }
    BitsUtil.flipI(hist, axis);
    // Process "higher" half (if nontrivial)
    if(hend - hstart > 1) {
      updateMinMax(mms, axis, half, max);

      // Compute gray code carry for (hist ^ refl)
      final boolean nextgray = gray ^ !is_reflected;
      // Update "first set bit in (hist ^ refl)"
      final int nextlast = !is_reflected ? depth : last;
      // LoggingUtil.warning("High: " + FormatUtil.format(mms) + " nextgray: " +
      // nextgray + " nextfirs: " + nextfirs);

      if(nextdepth > 0) {
        hilbertSort(objs, hstart, hend, mms, nextdepth, rotation, refl, hist, rev, nextgray, nextlast);
      }
      else {
        final int nextrot = (rotation + nextlast + 1) % dims;
        final boolean nrev = rev ^ !nextgray;
        LoggingUtil.warning("B old: " + rotation + " r: " + BitsUtil.toString(refl, dims) + " h: " + BitsUtil.toString(hist, dims) + " ffs: " + nextlast + " new: " + nextrot + " " + nrev);
        BitsUtil.flipI(hist, dims - 1 - rotation);
        hilbertSort(objs, hstart, hend, mms, nextdepth, nextrot, hist, BitsUtil.zero(dims), nrev, false, dims);
        BitsUtil.flipI(hist, dims - 1 - rotation);
      }
    }
    if(!is_reflected) {
      BitsUtil.flipI(hist, axis);
    }
    // Restore interval
    updateMinMax(mms, axis, min, max);
  }

  private void updateMinMax(double[] mms, int axis, double min, double max) {
    mms[2 * axis] = min;
    mms[2 * axis + 1] = max;
  }
}