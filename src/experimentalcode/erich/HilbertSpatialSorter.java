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
    hilbertSort(objs, start, end, minmax, 0, 0, 0L, 0L);
  }

  private <T extends SpatialComparable> void hilbertSort(List<T> objs, final int start, final int end, double[] mms, final int depth, final int axis, long history, long inversions) {
    final int dims = mms.length >>> 1;
    // Completed level of hilbert curve?
    final boolean complete = (depth + 1) % dims == 0;
    // final boolean right = (history & (1 << axis)) != 0;
    final boolean inv = (inversions & (1 << axis)) != 0;

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
    LoggingUtil.warning("Depth: " + depth + (complete ? " !" : " ") + " axis: " + (inv ? "-" : "+") + axis + /*
                                                                                                              * (
                                                                                                              * right
                                                                                                              * ?
                                                                                                              * "R"
                                                                                                              * :
                                                                                                              * " "
                                                                                                              * )
                                                                                                              * +
                                                                                                              */" history: " + history + " inversions: " + inversions + " " + FormatUtil.format(mms));
    int split = pivotizeList1D(objs, start, end, axis + 1, half, inv);
    // Need to descend?
    if(end - split <= 1 && split - start <= 1) {
      return;
    }

    if(!complete) {
      int nextaxis = (axis + 1) % dims;
      if(start < split - 1) {
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, nextaxis, history << 1, inversions);
      }
      if(split < end - 1) {
        inversions ^= 1L << nextaxis;
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, nextaxis, history << 1 ^ 1, inversions);
        inversions ^= 1L << nextaxis;
      }
    }
    else {
      int card = cardinality(history);
      boolean high = (history >> (dims - 2)) == 1;
      int leftaxis = bound(axis - card * (high ? -1 : 1), dims);
      int rightaxis = bound(axis - (card + 1) * (high ? -1 : 1), dims);
      LoggingUtil.warning("History: " + history + " card: " + card + " axis: " + (inv ? "-" : "+") + axis + " left: " + leftaxis + " right: " + rightaxis);
      LoggingUtil.warning("History igray: " + ungray(history) + " inversions igray: " + ungray(inversions));
      if(start < split - 1) {
        if((card % 2) == 1) {
          inversions ^= 1L << axis;
        }
        LoggingUtil.warning("History igray: " + ungray(history << 1) + " inversions igray: " + ungray(inversions));
        LoggingUtil.warning("LFlip: " + inversions + " ax:" + (inv ? "-" : "+") + axis + " lax:" + leftaxis + " card:" + card);
        mms[2 * axis] = !inv ? min : half;
        mms[2 * axis + 1] = !inv ? half : max;
        hilbertSort(objs, start, split, mms, depth + 1, leftaxis, 0L, inversions);
        if((card % 2) == 1) {
          inversions ^= 1L << axis;
        }
      }
      if(split < end - 1) {
        LoggingUtil.warning("History igray: " + ungray(1 + (history << 1)) + " inversions igray: " + ungray(inversions ^ 1L << rightaxis));
        LoggingUtil.warning("RFlip: " + inversions + " ax:" + (inv ? "-" : "+") + axis + " rax:" + rightaxis + " card:" + (card + 1));
        // inversions.flip(rightaxis);
        mms[2 * axis] = !inv ? half : min;
        mms[2 * axis + 1] = !inv ? max : half;
        hilbertSort(objs, split, end, mms, depth + 1, rightaxis, 1L, inversions);
        // inversions.flip(rightaxis);
      }
    }
    // Restore ranges
    mms[2 * axis] = min;
    mms[2 * axis + 1] = max;
    // FIXME: implement completely and test.
  }

  private long gray(long v) {
    return v ^ (v >>> 1);
  }

  private long ungray(long v) {
    v ^= (v >>> 1);
    v ^= (v >>> 2);
    v ^= (v >>> 4);
    v ^= (v >>> 8);
    v ^= (v >>> 16);
    v ^= (v >>> 32);
    return v;
  }

  private int bound(int val, int max) {
    while(val >= max) {
      val -= max;
    }
    while(val < 0) {
      val += max;
    }
    return val;
  }

  private int cardinality(long history) {
    int set = 0;
    while(history > 0) {
      if((history & 1) == 1) {
        set++;
      }
      history >>>= 1;
    }
    return set;
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
    bits &= -bits & nd1Ones;
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