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
    hilbertSort(objs, start, end, minmax, 0, 0, BitsUtil.zero(dims), BitsUtil.zero(dims));
  }

  private <T extends SpatialComparable> void hilbertSort(List<T> objs, final int start, final int end, double[] mms, final int depth, final int rotation, long[] coords, long[] reflections) {
    final int dims = mms.length >>> 1;
    // Completed level of hilbert curve?
    final boolean complete = (depth + 1) % dims == 0;
    final int axis = (depth + rotation) % dims;
    final boolean inv = BitsUtil.get(reflections, axis);

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
    // if(complete)
    LoggingUtil.warning("Depth: " + depth + (complete ? "!" : " ") + " axis: " + (inv ? "-" : "+") + axis + " coords: " + BitsUtil.toString(coords) + " " + " refl: " + BitsUtil.toString(reflections) + " " + FormatUtil.format(mms));
    int split = pivotizeList1D(objs, start, end, axis + 1, half, inv);
    // Need to descend?
    if(end - split <= 1 && split - start <= 1) {
      return;
    }

    if(complete) {
      BitsUtil.flipI(reflections, rotation);
      BitsUtil.flipI(reflections, 0);
      BitsUtil.xorI(coords, reflections);
      if(inv) {
        BitsUtil.flipI(coords, axis);
        BitsUtil.flipI(reflections, axis);
      }
      if(start < split - 1) {
        int rot = firstSetBit(coords, rotation, dims);
        final int nextrot = (rotation - rot + 1 + dims) % dims;
        LoggingUtil.warning("Rotation old: " + rotation + " c: " + BitsUtil.toString(coords) + " ffs: " + rot + " new: " + nextrot + " refl: " + BitsUtil.toString(reflections));
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, nextrot, BitsUtil.zero(dims), reflections);
      }
      BitsUtil.flipI(coords, axis);
      if(split < end - 1) {
        int rot = firstSetBit(coords, rotation, dims);
        final int nextrot = (rotation - rot + 1 + dims) % dims;
        LoggingUtil.warning("Rotation old: " + rotation + " c: " + BitsUtil.toString(coords) + " ffs: " + rot + " new: " + nextrot + " refl: " + BitsUtil.toString(reflections));
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, nextrot, BitsUtil.zero(dims), reflections);
      }
      if(!inv) {
        BitsUtil.flipI(coords, axis);
      }
      if(inv) {
        BitsUtil.flipI(reflections, axis);
      }
      BitsUtil.xorI(coords, reflections);
      BitsUtil.flipI(reflections, rotation);
      BitsUtil.flipI(reflections, 0);
    }
    else {
      if(inv) {
        BitsUtil.flipI(coords, axis);
      }
      if(start < split - 1) {
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, rotation, coords, reflections);
      }
      BitsUtil.flipI(coords, axis);
      if(split < end - 1) {
        final int nextaxis = (axis + 1) % dims;
        BitsUtil.flipI(reflections, nextaxis);
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, rotation, coords, reflections);
        BitsUtil.flipI(reflections, nextaxis);
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

  private int firstSetBit(long[] bitset, int start, int dims) {
    int l = BitsUtil.previousSetBit(bitset, start);
    if(l >= 0) {
      return start - l;
    }
    l = BitsUtil.previousSetBit(bitset, dims - 1);
    if(l >= 0) {
      return (dims - 1) - l + start;
    }
    return -1;
  }

  public static int bound(int val, int max) {
    while(val >= max) {
      val -= max;
    }
    while(val < 0) {
      val += max;
    }
    return val;
  }

  static long getIEEEBits(int nDims, double[] c, int y) {
    int d;
    long bits = 0L;
    for(double x = c[d = 0]; d < nDims; x = c[++d]) {
      // Disassemble, according to Java documentation:
      final long rawdouble = Double.doubleToRawLongBits(x);
      final int exponent = (int) ((rawdouble >> 52) & 0x7ffL);
      final long mantissa = (exponent == 0) ? (rawdouble & 0xfffffffffffffL) << 1 : (rawdouble & 0xfffffffffffffL) | 0x10000000000000L;
      boolean bit = (rawdouble >> 63) != 0; // negative
      boolean normalized = (exponent != 0);
      int diff = y - (exponent - (normalized ? 1 : 0));
      if(diff < 52) {
        bit ^= (1 & (mantissa >> diff)) == 1;
      }
      else if(diff == 52) {
        bit ^= normalized;
      }
      else {
        bit ^= (y == (1 << 11 + 51));
      }
      if(bit) {
        bits |= 1L << d;
      }
    }
    return bits;
  }

  static int hilbert_cmp_work(int nDims, int nBits, int max, int y, double[] c1, double[] c2, int rotation, long bits, long index) {
    while(y-- > max) {
      long reflection = getIEEEBits(nDims, c1, y);
      long diff = reflection ^ getIEEEBits(nDims, c2, y);
      bits ^= reflection;
      bits = cycleRight(bits, rotation, nDims);
      if(diff == 0) {
        diff = cycleRight(diff, rotation, nDims);
        // Gray-to-integer
        for(int d = 1; d < nDims; d *= 2) {
          index ^= index >> d;
          bits ^= bits >> d;
          diff ^= diff >> d;
        }
        final boolean foo = ((index ^ y ^ nBits) & 1) == 1;
        final boolean bar = bits < (bits ^ diff);
        return (foo == bar) ? -1 : 1;
      }
      index ^= bits;
      reflection ^= 1L << rotation;
      rotation = adjust_rotation(rotation, nDims, bits);
      bits = reflection;
    }
    return 0;
  }

  private static int adjust_rotation(int rotation, int nDims, long bits) {
    final long nd1Ones = (1 << (nDims - 1)) - 1;
    bits &= -bits & nd1Ones; // first set bit (0 if none or last)
    while(bits != 0) {
      bits >>>= 1;
      ++rotation;
    }
    if(++rotation >= nDims) {
      rotation -= nDims;
    }
    return rotation;
  }

  private static long cycleRight(long bits, int rotation, int nDims) {
    final long ones = (1 << nDims) - 1;
    return (((bits) >> (rotation)) | ((bits) << ((nDims) - (rotation)))) & ones;
  }
}