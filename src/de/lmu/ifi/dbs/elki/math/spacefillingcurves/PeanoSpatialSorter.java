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
import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Bulk-load an R-tree index by presorting the objects with their position on
 * the Peano curve.
 * 
 * The basic shape of this space-filling curve looks like this:
 * 
 * <pre>
 *   3---4   9
 *   |   |   |
 *   2   5   8
 *   |   |   |
 *   1   6---7
 * </pre>
 * 
 * Which then expands to the next level as:
 * 
 * <pre>
 *   +-+ +-+ +-+ +-+ E
 *   | | | | | | | | |
 *   | +-+ +-+ | | +-+
 *   |         | |    
 *   | +-+ +-+ | | +-+
 *   | | | | | | | | |
 *   +-+ | | +-+ +-+ |
 *       | |         |
 *   +-+ | | +-+ +-+ |
 *   | | | | | | | | |
 *   S +-+ +-+ +-+ +-+
 * </pre>
 * 
 * and so on.
 * 
 * Reference:
 * <p>
 * G. Peano<br />
 * Sur une courbe, qui remplit toute une aire plane<br />
 * Mathematische Annalen, 36(1)
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "G. Peano", title = "Sur une courbe, qui remplit toute une aire plane", booktitle = "Mathematische Annalen, 36(1)")
public class PeanoSpatialSorter extends AbstractSpatialSorter {
  /**
   * Constructor.
   */
  public PeanoSpatialSorter() {
    super();
  }

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs, int start, int end, double[] minmax) {
    peanoSort(objs, start, end, minmax, 0, new BitSet(), false);
  }

  /**
   * Sort by Peano curve.
   * 
   * @param objs Objects
   * @param start Start index
   * @param end End
   * @param mms Minmax values
   * @param dim Dimension
   * @param bits Bit set for inversions
   * @param desc Current ordering
   */
  protected <T extends SpatialComparable> void peanoSort(List<T> objs, int start, int end, double[] mms, int dim, BitSet bits, boolean desc) {
    final int dims = mms.length >> 1;
    // Find the splitting points.
    final double min = mms[2 * dim], max = mms[2 * dim + 1];
    final double tfirst = (min + min + max) / 3.;
    final double tsecond = (min + max + max) / 3.;
    // Safeguard against duplicate points:
    if(max - tsecond < 1E-10 || tsecond - tfirst < 1E-10 || tfirst - min < 1E-10) {
      boolean ok = false;
      for(int d = 0; d < mms.length; d += 2) {
        if(mms[d + 1] - mms[d] >= 1E-10) {
          ok = true;
          break;
        }
      }
      if(!ok) {
        return;
      }
    }
    final boolean inv = bits.get(dim) ^ desc;
    // Split the data set into three parts
    int fsplit, ssplit;
    if(!inv) {
      fsplit = pivotizeList1D(objs, start, end, dim, tfirst, false);
      ssplit = (fsplit < end - 1) ? pivotizeList1D(objs, fsplit, end, dim, tsecond, false) : fsplit;
    }
    else {
      fsplit = pivotizeList1D(objs, start, end, dim, tsecond, true);
      ssplit = (fsplit < end - 1) ? pivotizeList1D(objs, fsplit, end, dim, tfirst, true) : fsplit;
    }
    int nextdim = (dim + 1) % dims;
    // Do we need to update the min/max values?
    if(start < fsplit - 1) {
      mms[2 * dim] = !inv ? min : tsecond;
      mms[2 * dim + 1] = !inv ? tfirst : max;
      peanoSort(objs, start, fsplit, mms, nextdim, bits, desc);
    }
    if(fsplit < ssplit - 1) {
      bits.flip(dim); // set (all but dim: we also flip "desc")
      mms[2 * dim] = tfirst;
      mms[2 * dim + 1] = tsecond;
      peanoSort(objs, fsplit, ssplit, mms, nextdim, bits, !desc);
      bits.flip(dim);
    }
    if(ssplit < end - 1) {
      mms[2 * dim] = !inv ? tsecond : min;
      mms[2 * dim + 1] = !inv ? max : tfirst;
      peanoSort(objs, ssplit, end, mms, nextdim, bits, desc);
    }
    // Restore ranges
    mms[2 * dim] = min;
    mms[2 * dim + 1] = max;
  }
}