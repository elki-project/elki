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

import static de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter.pivotizeList1D;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Bulk-load an R-tree index by presorting the objects with their position on
 * the Peano curve.
 * <p>
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
 * <p>
 * Reference:
 * <p>
 * G. Peano<br>
 * Sur une courbe, qui remplit toute une aire plane<br>
 * Mathematische Annalen 36(1)
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "G. Peano", //
    title = "Sur une courbe, qui remplit toute une aire plane", //
    booktitle = "Mathematische Annalen 36(1)", //
    url = "http://resolver.sub.uni-goettingen.de/purl?GDZPPN002252376", //
    bibkey = "journals/mathann/Peano1890")
public class PeanoSpatialSorter implements SpatialSorter {
  /**
   * Static instance.
   */
  public static final PeanoSpatialSorter STATIC = new PeanoSpatialSorter();

  /**
   * Constructor, use {@link #STATIC} instead.
   */
  public PeanoSpatialSorter() {
    super();
  }

  @Override
  public void sort(List<? extends SpatialComparable> objs, int start, int end, double[] minmax, int[] dims) {
    peanoSort(objs, start, end, minmax, dims, 0, BitsUtil.zero(minmax.length >> 1), false);
  }

  /**
   * Sort by Peano curve.
   *
   * @param objs Objects
   * @param start Start index
   * @param end End
   * @param mms Minmax values
   * @param dims Dimensions index
   * @param depth Dimension
   * @param bits Bit set for inversions
   * @param desc Current ordering
   */
  protected void peanoSort(List<? extends SpatialComparable> objs, int start, int end, double[] mms, int[] dims, int depth, long[] bits, boolean desc) {
    final int numdim = (dims != null) ? dims.length : (mms.length >> 1);
    final int edim = (dims != null) ? dims[depth] : depth;
    // Find the splitting points.
    final double min = mms[2 * edim], max = mms[2 * edim + 1];
    final double tfirst = (min + min + max) / 3.;
    final double tsecond = (min + max + max) / 3.;
    // Safeguard against duplicate points:
    if(max - tsecond < 1E-10 || tsecond - tfirst < 1E-10 || tfirst - min < 1E-10) {
      boolean ok = false;
      for(int d = 0; d < numdim; d++) {
        int d2 = ((dims != null) ? dims[d] : d) << 1;
        if(mms[d2 + 1] - mms[d2] >= 1E-10) {
          ok = true;
          break;
        }
      }
      if(!ok) {
        return;
      }
    }
    final boolean inv = BitsUtil.get(bits, edim) ^ desc;
    // Split the data set into three parts
    int fsplit, ssplit;
    if(!inv) {
      fsplit = pivotizeList1D(objs, start, end, edim, tfirst, false);
      ssplit = (fsplit < end - 1) ? pivotizeList1D(objs, fsplit, end, edim, tsecond, false) : fsplit;
    }
    else {
      fsplit = pivotizeList1D(objs, start, end, edim, tsecond, true);
      ssplit = (fsplit < end - 1) ? pivotizeList1D(objs, fsplit, end, edim, tfirst, true) : fsplit;
    }
    int nextdim = (depth + 1) % numdim;
    // Do we need to update the min/max values?
    if(start < fsplit - 1) {
      mms[2 * edim] = !inv ? min : tsecond;
      mms[2 * edim + 1] = !inv ? tfirst : max;
      peanoSort(objs, start, fsplit, mms, dims, nextdim, bits, desc);
    }
    if(fsplit < ssplit - 1) {
      BitsUtil.flipI(bits, edim); // set (all but dim: we also flip "desc")
      mms[2 * edim] = tfirst;
      mms[2 * edim + 1] = tsecond;
      peanoSort(objs, fsplit, ssplit, mms, dims, nextdim, bits, !desc);
      BitsUtil.flipI(bits, edim);
    }
    if(ssplit < end - 1) {
      mms[2 * edim] = !inv ? tsecond : min;
      mms[2 * edim + 1] = !inv ? max : tfirst;
      peanoSort(objs, ssplit, end, mms, dims, nextdim, bits, desc);
    }
    // Restore ranges
    mms[2 * edim] = min;
    mms[2 * edim + 1] = max;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected PeanoSpatialSorter makeInstance() {
      return STATIC;
    }
  }
}
