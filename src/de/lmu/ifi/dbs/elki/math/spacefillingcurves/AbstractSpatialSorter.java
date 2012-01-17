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
 * Abstract base class for spatial sorters, offering shared functionality.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractSpatialSorter implements SpatialSorter {
  /**
   * Constructor.
   */
  public AbstractSpatialSorter() {
    super();
  }

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs) {
    double[] mms = computeMinMax(objs);
    sort(objs, 0, objs.size(), mms);
  }

  /**
   * "Pivotize" the list, such that all elements before the given position are
   * less than, all elements after the position are larger than the threshold
   * value in the given dimension. (desc inverts the sorting!)
   * 
   * Only the elments in the interval <tt>[start: end[</tt> are sorted!
   * 
   * @param objs List of objects
   * @param start Start of sorting range
   * @param end End of sorting range
   * @param dim Dimension to sort by
   * @param threshold Threshold value
   * @param desc Inversion flag
   * @return Pivot position
   */
  protected <T extends SpatialComparable> int pivotizeList1D(List<T> objs, int start, int end, int dim, double threshold, boolean desc) {
    threshold = 2 * threshold; // faster
    int s = start, e = end;
    while(s < e) {
      if(!desc) {
        double sminmax = getMinPlusMaxObject(objs, s, dim);
        while((sminmax < threshold) && s + 1 <= e && s + 1 < end) {
          s++;
          sminmax = getMinPlusMaxObject(objs, s, dim);
        }
        double eminmax = getMinPlusMaxObject(objs, e - 1, dim);
        while((eminmax >= threshold) && s < e - 1 && start < e - 1) {
          e--;
          eminmax = getMinPlusMaxObject(objs, e - 1, dim);
        }
      }
      else {
        double sminmax = getMinPlusMaxObject(objs, s, dim);
        while((sminmax > threshold) && s + 1 <= e && s + 1 < end) {
          s++;
          sminmax = getMinPlusMaxObject(objs, s, dim);
        }
        double eminmax = getMinPlusMaxObject(objs, e - 1, dim);
        while((eminmax <= threshold) && s < e - 1 && start < e - 1) {
          e--;
          eminmax = getMinPlusMaxObject(objs, e - 1, dim);
        }
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
    return e;
  }

  /**
   * Compute getMin(dim) + getMax(dim) for the spatial object
   * 
   * @param objs Objects
   * @param s index
   * @param dim Dimensionality
   * @return Min+Max
   */
  private double getMinPlusMaxObject(List<? extends SpatialComparable> objs, int s, int dim) {
    SpatialComparable sobj = objs.get(s);
    return sobj.getMin(dim) + sobj.getMax(dim);
  }

  /**
   * Compute the minimum and maximum for each dimension.
   * 
   * @param objs Objects
   * @return Array of min, max pairs (length = 2 * dim)
   */
  protected <T extends SpatialComparable> double[] computeMinMax(List<T> objs) {
    final int dim = objs.get(0).getDimensionality();
    // Compute min and max for each dimension:
    double[] mm = new double[dim * 2];
    {
      for(int d = 0; d < dim; d++) {
        mm[d * 2] = Double.POSITIVE_INFINITY;
        mm[d * 2 + 1] = Double.NEGATIVE_INFINITY;
      }
      for(SpatialComparable obj : objs) {
        for(int d = 0; d < dim; d++) {
          mm[2 * d] = Math.min(mm[2 * d], obj.getMin(d + 1));
          mm[2 * d + 1] = Math.max(mm[2 * d + 1], obj.getMax(d + 1));
        }
      }
      for(int d = 0; d < dim; d++) {
        assert (mm[2 * d] <= mm[2 * d + 1]);
      }
    }
    return mm;
  }
}