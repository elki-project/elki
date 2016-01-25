package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Class to sort the data set by their Z-index, without doing a full
 * materialization of the Z indexes.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ZCurveSpatialSorter extends AbstractSpatialSorter {
  private static final double STOPVAL = 1E-10;

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs, int start, int end, double[] minmax, int[] dims) {
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
  protected <T extends SpatialComparable> void zSort(List<T> objs, int start, int end, double[] mms, int[] dims, int depth) {
    final int numdim = (dims != null) ? dims.length : (mms.length >> 1);
    final int edim = (dims != null) ? dims[depth] : depth;
    // Find the splitting points.
    final double min = mms[2 * edim], max = mms[2 * edim + 1];
    double spos = (min + max) / 2.;
    // Safeguard against duplicate points:
    if (max - spos < STOPVAL || spos - min < STOPVAL) {
      boolean ok = false;
      for (int d = 0; d < numdim; d++) {
        int d2 = ((dims != null) ? dims[d] : d) << 1;
        if (mms[d2 + 1] - mms[d2] >= STOPVAL) {
          ok = true;
          break;
        }
      }
      if (!ok) {
        return;
      }
    }
    int split = pivotizeList1D(objs, start, end, edim, spos, false);
    assert (start <= split && split <= end);
    int nextdim = (depth + 1) % numdim;
    if (start < split - 1) {
      mms[2 * edim] = min;
      mms[2 * edim + 1] = spos;
      zSort(objs, start, split, mms, dims, nextdim);
    }
    if (split < end - 1) {
      mms[2 * edim] = spos;
      mms[2 * edim + 1] = max;
      zSort(objs, split, end, mms, dims, nextdim);
    }
    // Restore ranges
    mms[2 * edim] = min;
    mms[2 * edim + 1] = max;
  }
}
