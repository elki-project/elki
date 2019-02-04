/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Class to sort the data set by their Z-index, without doing a full
 * materialization of the Z indexes.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ZCurveSpatialSorter implements SpatialSorter {
  /**
   * Static instance.
   */
  public static final ZCurveSpatialSorter STATIC = new ZCurveSpatialSorter();

  /**
   * Stopping threshold.
   */
  private static final double STOPVAL = 1E-10;

  /**
   * Constructor, use {@link #STATIC} instead.
   */
  public ZCurveSpatialSorter() {
    super();
  }

  @Override
  public void sort(List<? extends SpatialComparable> objs, int start, int end, double[] minmax, int[] dims) {
    zSort(objs, start, end, minmax, dims, 0);
  }

  /**
   * The actual Z sorting function
   * 
   * @param objs Objects to sort
   * @param start Start
   * @param end End
   * @param mms Min-Max value ranges
   * @param dims Dimensions to process
   * @param depth Current dimension
   */
  protected void zSort(List<? extends SpatialComparable> objs, int start, int end, double[] mms, int[] dims, int depth) {
    final int numdim = (dims != null) ? dims.length : (mms.length >> 1);
    final int edim = (dims != null) ? dims[depth] : depth;
    // Find the splitting points.
    final double min = mms[2 * edim], max = mms[2 * edim + 1];
    double spos = (min + max) / 2.;
    // Safeguard against duplicate points:
    if(max - spos < STOPVAL || spos - min < STOPVAL) {
      boolean ok = false;
      for(int d = 0; d < numdim; d++) {
        int d2 = ((dims != null) ? dims[d] : d) << 1;
        if(mms[d2 + 1] - mms[d2] >= STOPVAL) {
          ok = true;
          break;
        }
      }
      if(!ok) {
        return;
      }
    }
    int split = pivotizeList1D(objs, start, end, edim, spos, false);
    assert (start <= split && split <= end);
    int nextdim = (depth + 1) % numdim;
    if(start < split - 1) {
      mms[2 * edim] = min;
      mms[2 * edim + 1] = spos;
      zSort(objs, start, split, mms, dims, nextdim);
    }
    if(split < end - 1) {
      mms[2 * edim] = spos;
      mms[2 * edim + 1] = max;
      zSort(objs, split, end, mms, dims, nextdim);
    }
    // Restore ranges
    mms[2 * edim] = min;
    mms[2 * edim + 1] = max;
  }

  /**
   * "Pivotize" the list, such that all elements before the given position are
   * less than, all elements after the position are larger than the threshold
   * value in the given dimension. (desc inverts the sorting!)
   * 
   * Only the elements in the interval <tt>[start: end[</tt> are sorted!
   * 
   * @param objs List of objects
   * @param start Start of sorting range
   * @param end End of sorting range
   * @param dim Dimension to sort by
   * @param threshold Threshold value
   * @param desc Inversion flag
   * @return Pivot position
   */
  protected static int pivotizeList1D(List<? extends SpatialComparable> objs, int start, int end, int dim, double threshold, boolean desc) {
    @SuppressWarnings("unchecked") // Hack, for swapping elements.
    final List<SpatialComparable> sobjs = (List<SpatialComparable>) objs;
    threshold = 2 * threshold; // because we use minPlusMax coordinates below
    int s = start, e = end - 1;
    while(s <= e) {
      if(!desc) {
        double sminmax = getMinPlusMaxObject(objs, s, dim);
        while(sminmax < threshold && s <= e) {
          sminmax = ++s <= e ? getMinPlusMaxObject(objs, s, dim) : Double.POSITIVE_INFINITY;
        }
        double eminmax = getMinPlusMaxObject(objs, e, dim);
        while(eminmax >= threshold && s <= e) {
          eminmax = --e >= s ? getMinPlusMaxObject(objs, e, dim) : Double.NEGATIVE_INFINITY;
        }
      }
      else {
        double sminmax = getMinPlusMaxObject(objs, s, dim);
        while(sminmax > threshold && s <= e) {
          sminmax = ++s <= e ? getMinPlusMaxObject(objs, s, dim) : Double.POSITIVE_INFINITY;
        }
        double eminmax = getMinPlusMaxObject(objs, e, dim);
        while(eminmax <= threshold && s <= e) {
          eminmax = --e >= s ? getMinPlusMaxObject(objs, e, dim) : Double.NEGATIVE_INFINITY;
        }
      }
      if(s >= e) {
        break;
      }
      // Swap
      sobjs.set(s, sobjs.set(e, sobjs.get(s)));
      ++s;
      --e;
    }
    return s;
  }

  /**
   * Compute getMin(dim) + getMax(dim) for the spatial object.
   * 
   * @param objs Objects
   * @param s index
   * @param dim Dimensionality
   * @return Min+Max
   */
  private static double getMinPlusMaxObject(List<? extends SpatialComparable> objs, int s, int dim) {
    SpatialComparable sobj = objs.get(s);
    return sobj.getMin(dim) + sobj.getMax(dim);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ZCurveSpatialSorter makeInstance() {
      return STATIC;
    }
  }
}
