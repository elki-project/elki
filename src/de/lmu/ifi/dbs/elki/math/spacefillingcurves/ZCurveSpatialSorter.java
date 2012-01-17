package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

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

/**
 * Class to sort the data set by their Z-index, without doing a full
 * materialization of the Z indexes.
 * 
 * @author Erich Schubert
 */
public class ZCurveSpatialSorter extends AbstractSpatialSorter {
  private static final double STOPVAL = 1E-10;

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs, int start, int end, double[] minmax) {
    zSort(objs, start, end, minmax, 0);
  }

  /**
   * The actual Z sorting function
   * 
   * @param objs Objects to sort
   * @param start Start
   * @param end End
   * @param mms Min-Max value ranges
   * @param dim Current dimension
   */
  protected <T extends SpatialComparable> void zSort(List<T> objs, int start, int end, double[] mms, int dim) {
    // Find the splitting points.
    final double min = mms[2 * dim], max = mms[2 * dim + 1];
    double spos = (min + max) / 2.;
    // Safeguard against duplicate points:
    if(max - spos < STOPVAL || spos - min < STOPVAL) {
      boolean ok = false;
      for(int d = 0; d < mms.length; d += 2) {
        if(mms[d + 1] - mms[d] >= STOPVAL) {
          // LoggingUtil.warning("No: " + (mms[d + 1] - mms[d]));
          ok = true;
          break;
        }
      }
      if(!ok) {
        return;
      }
    }
    int split = pivotizeList1D(objs, start, end, dim + 1, spos, false);
    assert (start <= split && split <= end);
    int nextdim = (dim + 1) % objs.get(0).getDimensionality();
    // LoggingUtil.warning("dim: " + dim + " min: " + min + " split: " + spos +
    // " max:" + max + " " + start + " < " + split + " < " + end);
    if(start < split - 1) {
      mms[2 * dim] = min;
      mms[2 * dim + 1] = spos;
      zSort(objs, start, split, mms, nextdim);
    }
    if(split < end - 1) {
      mms[2 * dim] = spos;
      mms[2 * dim + 1] = max;
      zSort(objs, split, end, mms, nextdim);
    }
    // Restore ranges
    mms[2 * dim] = min;
    mms[2 * dim + 1] = max;
    // FIXME: implement completely and test.
  }
}